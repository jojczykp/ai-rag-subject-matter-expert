package org.alterbit.aisme.chat.runtime.embedded

import io.kotest.matchers.collections.shouldContainExactly
import java.time.Duration
import org.alterbit.aisme.chat.catalog.ChatProperties
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.alterbit.aisme.chat.catalog.ChatModelProperties
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeProperties
import org.alterbit.aisme.chat.catalog.ChatModelsProperties
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeConfigProperties
import org.junit.jupiter.api.Test

class EmbeddedLlamaAiModelClientProviderTest {
    @Test
    fun `creates one client per configured embedded offline model`() {
        val factory = FakeLlamaServerChatApiFactory()
        val provider = EmbeddedLlamaAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "embedded-qwen", displayOrder = 10),
                ollamaModel(id = "local-llama"),
                embeddedModel(id = "embedded-mistral", displayOrder = 20),
            ),
            chatProperties = ChatProperties(apiTimeout = Duration.ofSeconds(30)),
            embeddedLlamaProcessManager = processManager(
                ports = intArrayOf(19001, 19002),
            ),
            llamaServerChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("embedded-qwen", "embedded-mistral")
        factory.createdClients shouldContainExactly listOf(
            CreatedLlamaServerClient(
                baseUrl = "http://127.0.0.1:19001",
                apiTimeout = Duration.ofSeconds(30),
            ),
            CreatedLlamaServerClient(
                baseUrl = "http://127.0.0.1:19002",
                apiTimeout = Duration.ofSeconds(30),
            ),
        )
    }

    @Test
    fun `does not create clients for disabled embedded llama models`() {
        val factory = FakeLlamaServerChatApiFactory()
        val provider = EmbeddedLlamaAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-qwen", enabled = false)),
            chatProperties = ChatProperties(),
            embeddedLlamaProcessManager = processManager(
                modelIds = emptyList(),
                ports = intArrayOf(19001),
            ),
            llamaServerChatApiFactory = factory,
        )

        provider.clients() shouldContainExactly emptyList()
        factory.createdClients shouldContainExactly emptyList()
    }

    @Test
    fun `skips embedded chat models when runtime process is not ready`() {
        val factory = FakeLlamaServerChatApiFactory()
        val provider = EmbeddedLlamaAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "configured-runtime-model"),
                embeddedModel(id = "missing-runtime-model"),
            ),
            chatProperties = ChatProperties(),
            embeddedLlamaProcessManager = processManager(
                modelIds = listOf("configured-runtime-model"),
                ports = intArrayOf(19001),
            ),
            llamaServerChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("configured-runtime-model")
    }

    private fun chatModelRegistry(vararg models: Pair<String, ChatModelProperties>): ChatModelRegistry =
        ChatModelRegistry(
            ChatModelsProperties(
                chatRuntimesById = mapOf(
                    "embedded-llama" to ChatModelRuntimeConfigProperties(
                        type = ChatModelRuntime.EMBEDDED_LLAMA,
                        assetDirectory = "./models/llama",
                        serverExecutablePath = "./models/llama/bin/llama-server",
                    ),
                    "local-ollama" to ChatModelRuntimeConfigProperties(
                        type = ChatModelRuntime.OLLAMA,
                        baseUrl = "http://localhost:11434",
                    ),
                ),
                chatModelsById = models.toMap().withFallbackModelWhenNoneEnabled(),
            ),
        )

    private fun Map<String, ChatModelProperties>.withFallbackModelWhenNoneEnabled(): Map<String, ChatModelProperties> =
        if (values.any { it.enabled }) this else this + ollamaModel(id = "local-llama")

    private fun embeddedModel(
        id: String,
        enabled: Boolean = true,
        displayOrder: Int? = null,
    ): Pair<String, ChatModelProperties> =
        id to ChatModelProperties(
            enabled = enabled,
            displayOrder = displayOrder,
            displayName = "Embedded Qwen",
            runtime = ChatModelRuntimeProperties(
                id = "embedded-llama",
                modelName = "qwen2.5",
                ggufFile = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                contextSize = 4096,
            ),
        )

    private fun ollamaModel(id: String): Pair<String, ChatModelProperties> =
        id to ChatModelProperties(
            enabled = true,
            displayName = "Local Llama",
            runtime = ChatModelRuntimeProperties(
                id = "local-ollama",
                modelName = "llama3.2",
            ),
        )

    private fun processManager(
        modelIds: List<String> = listOf("embedded-qwen", "embedded-mistral"),
        ports: IntArray,
    ): EmbeddedLlamaProcessManager =
        EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(
                *modelIds.mapIndexed { index, id ->
                    embeddedModel(id = id, displayOrder = index)
                }.toTypedArray(),
            ),
            portAllocator = fixedPortAllocator(*ports),
            processLauncher = FakeEmbeddedLlamaProcessLauncher(),
            readinessProbe = LlamaServerReadinessProbe { _, _ -> true },
            processOutputLogger = EmbeddedLlamaProcessOutputLogger(lineConsumer = { _, _, _ -> }),
        )

    private fun fixedPortAllocator(vararg ports: Int): EphemeralEmbeddedLlamaPortAllocator {
        val remainingPorts = ports.toMutableList()
        return EphemeralEmbeddedLlamaPortAllocator { remainingPorts.removeFirst() }
    }

    private class FakeLlamaServerChatApiFactory : LlamaServerChatApiFactory {
        val createdClients = mutableListOf<CreatedLlamaServerClient>()

        override fun create(baseUrl: String, apiTimeout: Duration): LlamaServerChatApi {
            createdClients += CreatedLlamaServerClient(baseUrl = baseUrl, apiTimeout = apiTimeout)
            return FakeLlamaServerChatApi()
        }
    }

    private class FakeLlamaServerChatApi : LlamaServerChatApi {
        override fun complete(request: LlamaServerCompletionRequest): LlamaServerCompletionResponse =
            LlamaServerCompletionResponse(content = "Fake embedded answer")
    }

    private class FakeEmbeddedLlamaProcessLauncher : EmbeddedLlamaProcessLauncher {
        override fun start(command: List<String>): Process =
            error("Provider tests do not start managed processes")
    }

    private data class CreatedLlamaServerClient(
        val baseUrl: String,
        val apiTimeout: Duration,
    )
}
