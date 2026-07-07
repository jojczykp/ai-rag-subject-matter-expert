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
        val provider = LlamaRuntimeAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "embedded-llama"),
                ollamaModel(id = "local-llama"),
                embeddedModel(id = "embedded-qwen"),
            ),
            chatProperties = ChatProperties(timeout = Duration.ofSeconds(30)),
            llamaRuntimeProperties = enabledLlamaRuntimeProperties(
                runtimeModel(id = "embedded-llama"),
                runtimeModel(id = "embedded-qwen"),
            ),
            llamaServerChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("embedded-llama", "embedded-qwen")
        factory.createdClients shouldContainExactly listOf(
            CreatedLlamaServerClient(
                baseUrl = "http://127.0.0.1:18080",
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
            llamaServerChatApiFactory = factory,
        )

        provider.clients() shouldContainExactly emptyList()
        factory.createdClients shouldContainExactly emptyList()
    }

    @Test
    fun `skips embedded chat models without matching runtime model`() {
        val factory = FakeLlamaServerChatApiFactory()
        val provider = LlamaRuntimeAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "configured-runtime-model"),
                embeddedModel(id = "missing-runtime-model"),
            ),
            chatProperties = ChatProperties(),
            llamaRuntimeProperties = enabledLlamaRuntimeProperties(runtimeModel(id = "configured-runtime-model")),
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
                port = 18080,
                models = models.toList(),
            ),
        )

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

    private data class CreatedLlamaServerClient(
        val baseUrl: String,
        val timeout: Duration,
    )
}
