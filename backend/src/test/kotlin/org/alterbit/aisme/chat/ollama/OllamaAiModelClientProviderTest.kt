package org.alterbit.aisme.chat.ollama

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import java.time.Instant
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelMode
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelProperties
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelsProperties
import org.alterbit.aisme.modelcatalog.EnabledChatModelProperties
import org.junit.jupiter.api.Test
import org.springframework.ai.ollama.api.OllamaApi

class OllamaAiModelClientProviderTest {
    @Test
    fun `creates one client per configured ollama model`() {
        val factory = FakeOllamaChatApiFactory()
        val provider = OllamaAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                ollamaModel(id = "local-llama", modelName = "llama3.2"),
                springAiModel(id = "cloud-gpt"),
                ollamaModel(id = "local-qwen", baseUrl = "http://localhost:11435", modelName = "qwen2.5"),
            ),
            chatProperties = ChatProperties(timeout = Duration.ofSeconds(30)),
            ollamaChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("local-llama", "local-qwen")
        factory.createdClients shouldContainExactly listOf(
            CreatedOllamaClient(
                baseUrl = "http://localhost:11434",
                timeout = Duration.ofSeconds(30),
            ),
            CreatedOllamaClient(
                baseUrl = "http://localhost:11435",
                timeout = Duration.ofSeconds(30),
            ),
        )
    }

    @Test
    fun `rejects ollama model without base url`() {
        val exception = shouldThrow<IllegalArgumentException> {
            OllamaAiModelClientProvider(
                chatModelRegistry = chatModelRegistry(ollamaModel(baseUrl = null)),
                chatProperties = ChatProperties(),
                ollamaChatApiFactory = FakeOllamaChatApiFactory(),
            )
        }

        exception.message shouldContain "aisme.chat-models[0].config.base-url"
        exception.message shouldContain "is required"
    }

    private fun chatModelRegistry(vararg models: ConfiguredChatModelProperties): ChatModelRegistry =
        ChatModelRegistry(ConfiguredChatModelsProperties(chatModels = models.toList()))

    private fun ollamaModel(
        id: String = "local-llama",
        baseUrl: String? = "http://localhost:11434",
        modelName: String? = "llama3.2",
    ): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            enabled = true,
            config = EnabledChatModelProperties(
                displayName = "Local Llama",
                runtime = ChatModelRuntime.OLLAMA,
                mode = ChatModelMode.LOCAL_SERVER,
                availableOffline = false,
                baseUrl = baseUrl,
                modelName = modelName,
            ),
        )

    private fun springAiModel(id: String): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            enabled = true,
            config = EnabledChatModelProperties(
                displayName = "Cloud GPT",
                runtime = ChatModelRuntime.SPRING_AI,
                mode = ChatModelMode.ONLINE,
                availableOffline = false,
            ),
        )

    private class FakeOllamaChatApiFactory : OllamaChatApiFactory {
        val createdClients = mutableListOf<CreatedOllamaClient>()

        override fun create(baseUrl: String, timeout: Duration): OllamaChatApi {
            createdClients += CreatedOllamaClient(baseUrl = baseUrl, timeout = timeout)
            return FakeOllamaChatApi()
        }
    }

    private class FakeOllamaChatApi : OllamaChatApi {
        override fun chat(request: OllamaApi.ChatRequest): OllamaApi.ChatResponse =
            OllamaApi.ChatResponse(
                request.model(),
                Instant.EPOCH,
                OllamaApi.Message.builder(OllamaApi.Message.Role.ASSISTANT)
                    .content("Fake Ollama answer")
                    .build(),
                "stop",
                true,
                0L,
                0L,
                0,
                0L,
                0,
                0L,
            )

        override fun modelNames(): Set<String> =
            emptySet()
    }

    private data class CreatedOllamaClient(
        val baseUrl: String,
        val timeout: Duration,
    )
}
