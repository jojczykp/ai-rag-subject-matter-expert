package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.collections.shouldContainExactly
import java.time.Duration
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelMode
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelProperties
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelsProperties
import org.alterbit.aisme.modelcatalog.EnabledChatModelProperties
import org.junit.jupiter.api.Test

class EmbeddedLlamaAiModelClientProviderTest {
    @Test
    fun `creates one client per configured embedded offline model`() {
        val factory = FakeLlamaServerChatApiFactory()
        val embeddedLlamaProperties = embeddedLlamaProperties(
            runtimeModel(id = "embedded-llama", enabled = true),
            runtimeModel(id = "embedded-qwen", enabled = true),
        )
        val provider = EmbeddedLlamaAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "embedded-llama"),
                ollamaModel(id = "local-llama"),
                embeddedModel(id = "embedded-qwen"),
            ),
            chatProperties = ChatProperties(timeout = Duration.ofSeconds(30)),
            embeddedLlamaProperties = embeddedLlamaProperties,
            embeddedLlamaProcessManager = processManager(
                embeddedLlamaProperties = embeddedLlamaProperties,
                ports = intArrayOf(19001, 19002),
            ),
            llamaServerChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("embedded-llama", "embedded-qwen")
        factory.createdClients shouldContainExactly listOf(
            CreatedLlamaServerClient(
                baseUrl = "http://127.0.0.1:19001",
                timeout = Duration.ofSeconds(30),
            ),
            CreatedLlamaServerClient(
                baseUrl = "http://127.0.0.1:19002",
                timeout = Duration.ofSeconds(30),
            ),
        )
    }

    @Test
    fun `does not create clients for disabled embedded llama models`() {
        val factory = FakeLlamaServerChatApiFactory()
        val embeddedLlamaProperties = embeddedLlamaProperties(runtimeModel(id = "embedded-llama", enabled = false))
        val provider = EmbeddedLlamaAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-llama")),
            chatProperties = ChatProperties(),
            embeddedLlamaProperties = embeddedLlamaProperties,
            embeddedLlamaProcessManager = processManager(
                embeddedLlamaProperties = embeddedLlamaProperties,
                ports = intArrayOf(19001),
            ),
            llamaServerChatApiFactory = factory,
        )

        provider.clients() shouldContainExactly emptyList()
        factory.createdClients shouldContainExactly emptyList()
    }

    @Test
    fun `skips embedded chat models without matching runtime model`() {
        val factory = FakeLlamaServerChatApiFactory()
        val embeddedLlamaProperties = embeddedLlamaProperties(
            runtimeModel(id = "configured-runtime-model", enabled = true),
        )
        val provider = EmbeddedLlamaAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "configured-runtime-model"),
                embeddedModel(id = "missing-runtime-model"),
            ),
            chatProperties = ChatProperties(),
            embeddedLlamaProperties = embeddedLlamaProperties,
            embeddedLlamaProcessManager = processManager(
                embeddedLlamaProperties = embeddedLlamaProperties,
                ports = intArrayOf(19001),
            ),
            llamaServerChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("configured-runtime-model")
    }

    private fun chatModelRegistry(vararg models: ConfiguredChatModelProperties): ChatModelRegistry =
        ChatModelRegistry(ConfiguredChatModelsProperties(chatModels = models.toList()))

    private fun embeddedModel(id: String): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            enabled = true,
            config = EnabledChatModelProperties(
                displayName = "Embedded Llama",
                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                mode = ChatModelMode.EMBEDDED_OFFLINE,
                availableOffline = true,
            ),
        )

    private fun ollamaModel(id: String): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            enabled = true,
            config = EnabledChatModelProperties(
                displayName = "Local Llama",
                runtime = ChatModelRuntime.OLLAMA,
                mode = ChatModelMode.LOCAL_SERVER,
                availableOffline = false,
                baseUrl = "http://localhost:11434",
                modelName = "llama3.2",
            ),
        )

    private fun embeddedLlamaProperties(
        vararg models: EmbeddedLlamaModelProperties,
    ): EmbeddedLlamaProperties =
        EmbeddedLlamaProperties(
            assetDirectory = "./models/llama",
            serverExecutablePath = "./models/llama/bin/llama-server",
            models = models.toList(),
        )

    private fun processManager(
        embeddedLlamaProperties: EmbeddedLlamaProperties,
        ports: IntArray,
    ): EmbeddedLlamaProcessManager =
        EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "embedded-llama"),
                embeddedModel(id = "embedded-qwen"),
                embeddedModel(id = "configured-runtime-model"),
                embeddedModel(id = "missing-runtime-model"),
            ),
            embeddedLlamaProperties = embeddedLlamaProperties,
            portAllocator = fixedPortAllocator(*ports),
            processLauncher = FakeEmbeddedLlamaProcessLauncher(),
            readinessProbe = LlamaServerReadinessProbe { _, _ -> true },
            processOutputLogger = EmbeddedLlamaProcessOutputLogger(lineConsumer = { _, _, _ -> }),
        )

    private fun fixedPortAllocator(vararg ports: Int): EphemeralEmbeddedLlamaPortAllocator {
        val remainingPorts = ports.toMutableList()
        return EphemeralEmbeddedLlamaPortAllocator { remainingPorts.removeFirst() }
    }

    private fun runtimeModel(id: String, enabled: Boolean = false): EmbeddedLlamaModelProperties =
        EmbeddedLlamaModelProperties(
            id = id,
            enabled = enabled,
            displayName = "Embedded Llama",
            ggufFile = "llama.gguf",
            contextSize = 4096,
        )

    private class FakeLlamaServerChatApiFactory : LlamaServerChatApiFactory {
        val createdClients = mutableListOf<CreatedLlamaServerClient>()

        override fun create(baseUrl: String, timeout: Duration): LlamaServerChatApi {
            createdClients += CreatedLlamaServerClient(baseUrl = baseUrl, timeout = timeout)
            return FakeLlamaServerChatApi()
        }
    }

    private class FakeLlamaServerChatApi : LlamaServerChatApi {
        override fun chat(request: LlamaServerChatRequest): LlamaServerChatResponse =
            LlamaServerChatResponse(
                choices = listOf(
                    LlamaServerChatChoice(
                        message = LlamaServerChatMessage(
                            role = "assistant",
                            content = "Fake embedded answer",
                        ),
                    ),
                ),
            )
    }

    private class FakeEmbeddedLlamaProcessLauncher : EmbeddedLlamaProcessLauncher {
        override fun start(command: List<String>): Process =
            error("Provider tests do not start managed processes")
    }

    private data class CreatedLlamaServerClient(
        val baseUrl: String,
        val timeout: Duration,
    )
}
