package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.collections.shouldContainExactly
import java.time.Duration
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chatmodel.ChatModelMode
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.alterbit.aisme.chatmodel.ConfiguredChatModelProperties
import org.alterbit.aisme.chatmodel.ConfiguredChatModelsProperties
import org.alterbit.aisme.chatmodel.EnabledChatModelProperties
import org.junit.jupiter.api.Test

class LlamaRuntimeAiModelClientProviderTest {
    @Test
    fun `creates one client per configured embedded offline model`() {
        val factory = FakeLlamaServerChatApiFactory()
        val llamaRuntimeProperties = enabledLlamaRuntimeProperties(
            runtimeModel(id = "embedded-llama"),
            runtimeModel(id = "embedded-qwen"),
        )
        val provider = LlamaRuntimeAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "embedded-llama"),
                ollamaModel(id = "local-llama"),
                embeddedModel(id = "embedded-qwen"),
            ),
            chatProperties = ChatProperties(timeout = Duration.ofSeconds(30)),
            llamaRuntimeProperties = llamaRuntimeProperties,
            llamaRuntimeProcessManager = processManager(
                llamaRuntimeProperties = llamaRuntimeProperties,
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
    fun `does not create clients when llama runtime is disabled`() {
        val factory = FakeLlamaServerChatApiFactory()
        val provider = LlamaRuntimeAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-llama")),
            chatProperties = ChatProperties(),
            llamaRuntimeProperties = LlamaRuntimeProperties(enabled = false),
            llamaRuntimeProcessManager = processManager(
                llamaRuntimeProperties = LlamaRuntimeProperties(enabled = false),
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
        val llamaRuntimeProperties = enabledLlamaRuntimeProperties(runtimeModel(id = "configured-runtime-model"))
        val provider = LlamaRuntimeAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "configured-runtime-model"),
                embeddedModel(id = "missing-runtime-model"),
            ),
            chatProperties = ChatProperties(),
            llamaRuntimeProperties = llamaRuntimeProperties,
            llamaRuntimeProcessManager = processManager(
                llamaRuntimeProperties = llamaRuntimeProperties,
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

    private fun enabledLlamaRuntimeProperties(
        vararg models: LlamaRuntimeModelProperties,
    ): LlamaRuntimeProperties =
        LlamaRuntimeProperties(
            enabled = true,
            config = EnabledLlamaRuntimeProperties(
                assetDirectory = "./models/llama",
                serverExecutablePath = "./models/llama/bin/llama-server",
                models = models.toList(),
            ),
        )

    private fun processManager(
        llamaRuntimeProperties: LlamaRuntimeProperties,
        ports: IntArray,
    ): LlamaRuntimeProcessManager =
        LlamaRuntimeProcessManager(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "embedded-llama"),
                embeddedModel(id = "embedded-qwen"),
                embeddedModel(id = "configured-runtime-model"),
                embeddedModel(id = "missing-runtime-model"),
            ),
            llamaRuntimeProperties = llamaRuntimeProperties,
            portAllocator = fixedPortAllocator(*ports),
            processLauncher = FakeLlamaRuntimeProcessLauncher(),
            readinessProbe = LlamaServerReadinessProbe { _, _ -> true },
            processOutputLogger = LlamaRuntimeProcessOutputLogger(lineConsumer = { _, _, _ -> }),
        )

    private fun fixedPortAllocator(vararg ports: Int): EphemeralLlamaRuntimePortAllocator {
        val remainingPorts = ports.toMutableList()
        return EphemeralLlamaRuntimePortAllocator { remainingPorts.removeFirst() }
    }

    private fun runtimeModel(id: String): LlamaRuntimeModelProperties =
        LlamaRuntimeModelProperties(
            id = id,
            displayName = "Embedded Llama",
            ggufFile = "llama.gguf",
            contextSize = 4096,
            license = "Apache-2.0",
            hardwareRequirements = "8 GB RAM",
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

    private class FakeLlamaRuntimeProcessLauncher : LlamaRuntimeProcessLauncher {
        override fun start(command: List<String>): Process =
            error("Provider tests do not start managed processes")
    }

    private data class CreatedLlamaServerClient(
        val baseUrl: String,
        val timeout: Duration,
    )
}
