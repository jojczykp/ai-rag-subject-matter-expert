package org.alterbit.aisme.chat.huggingface

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelMode
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelProperties
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelsProperties
import org.alterbit.aisme.modelcatalog.EnabledChatModelProperties
import org.junit.jupiter.api.Test

class HuggingFaceTgiAiModelClientProviderTest {
    @Test
    fun `creates one client per configured Hugging Face endpoint model`() {
        val factory = FakeHuggingFaceTgiChatApiFactory()
        val provider = HuggingFaceTgiAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                huggingFaceModel(id = "hf-mistral", baseUrl = "https://hf.example.com", apiKey = "test-api-key"),
                nonHuggingFaceModel(id = "local-llama"),
                huggingFaceModel(id = "hf-qwen", baseUrl = "https://qwen.example.com", apiKey = null),
            ),
            chatProperties = ChatProperties(timeout = Duration.ofSeconds(30)),
            huggingFaceTgiChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("hf-mistral", "hf-qwen")
        factory.createdClients shouldContainExactly listOf(
            CreatedHuggingFaceTgiClient(
                baseUrl = "https://hf.example.com",
                apiKey = "test-api-key",
                timeout = Duration.ofSeconds(30),
            ),
            CreatedHuggingFaceTgiClient(
                baseUrl = "https://qwen.example.com",
                apiKey = null,
                timeout = Duration.ofSeconds(30),
            ),
        )
    }

    @Test
    fun `rejects Hugging Face endpoint model without base url`() {
        val exception = shouldThrow<IllegalArgumentException> {
            HuggingFaceTgiAiModelClientProvider(
                chatModelRegistry = chatModelRegistry(huggingFaceModel(baseUrl = null)),
                chatProperties = ChatProperties(),
                huggingFaceTgiChatApiFactory = FakeHuggingFaceTgiChatApiFactory(),
            )
        }

        exception.message shouldContain "aisme.chat-models[0].config.base-url"
        exception.message shouldContain "is required"
    }

    private fun chatModelRegistry(vararg models: ConfiguredChatModelProperties): ChatModelRegistry =
        ChatModelRegistry(ConfiguredChatModelsProperties(chatModels = models.toList()))

    private fun huggingFaceModel(
        id: String = "hf-mistral",
        baseUrl: String? = "https://hf.example.com",
        apiKey: String? = "test-api-key",
    ): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            enabled = true,
            config = EnabledChatModelProperties(
                displayName = "Hugging Face Mistral",
                runtime = ChatModelRuntime.HUGGING_FACE_ENDPOINT,
                mode = ChatModelMode.ONLINE,
                availableOffline = false,
                baseUrl = baseUrl,
                modelName = null,
                apiKey = apiKey,
            ),
        )

    private fun nonHuggingFaceModel(id: String): ConfiguredChatModelProperties =
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

    private class FakeHuggingFaceTgiChatApiFactory : HuggingFaceTgiChatApiFactory {
        val createdClients = mutableListOf<CreatedHuggingFaceTgiClient>()

        override fun create(
            baseUrl: String,
            apiKey: String?,
            timeout: Duration,
        ): HuggingFaceTgiChatApi {
            createdClients += CreatedHuggingFaceTgiClient(
                baseUrl = baseUrl,
                apiKey = apiKey,
                timeout = timeout,
            )
            return FakeHuggingFaceTgiChatApi()
        }
    }

    private class FakeHuggingFaceTgiChatApi : HuggingFaceTgiChatApi {
        override fun generate(request: HuggingFaceTgiGenerateRequest): HuggingFaceTgiGenerateResponse =
            HuggingFaceTgiGenerateResponse(generatedText = "Fake Hugging Face answer")
    }

    private data class CreatedHuggingFaceTgiClient(
        val baseUrl: String,
        val apiKey: String?,
        val timeout: Duration,
    )
}
