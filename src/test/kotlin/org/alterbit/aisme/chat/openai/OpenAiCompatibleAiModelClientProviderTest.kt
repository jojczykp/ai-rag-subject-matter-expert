package org.alterbit.aisme.chat.openai

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chatmodel.ChatModelMode
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.alterbit.aisme.chatmodel.ConfiguredChatModelProperties
import org.alterbit.aisme.chatmodel.ConfiguredChatModelsProperties
import org.junit.jupiter.api.Test

class OpenAiCompatibleAiModelClientProviderTest {
    @Test
    fun `creates one client per configured OpenAI-compatible model`() {
        val factory = FakeOpenAiCompatibleChatApiFactory()
        val provider = OpenAiCompatibleAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                openAiModel(id = "cloud-gpt", modelName = "gpt-4.1-mini"),
                ollamaModel(id = "local-llama"),
                openAiModel(id = "cloud-qwen", baseUrl = "https://gateway.example.com/v1", modelName = "qwen-plus"),
            ),
            chatProperties = ChatProperties(timeout = Duration.ofSeconds(30)),
            openAiCompatibleChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("cloud-gpt", "cloud-qwen")
        factory.createdClients shouldContainExactly listOf(
            CreatedOpenAiCompatibleClient(
                baseUrl = "https://api.example.com/v1",
                apiKey = "test-api-key",
                timeout = Duration.ofSeconds(30),
            ),
            CreatedOpenAiCompatibleClient(
                baseUrl = "https://gateway.example.com/v1",
                apiKey = "test-api-key",
                timeout = Duration.ofSeconds(30),
            ),
        )
    }

    @Test
    fun `rejects OpenAI-compatible model without base url`() {
        val exception = shouldThrow<IllegalStateException> {
            OpenAiCompatibleAiModelClientProvider(
                chatModelRegistry = chatModelRegistry(openAiModel(baseUrl = null)),
                chatProperties = ChatProperties(),
                openAiCompatibleChatApiFactory = FakeOpenAiCompatibleChatApiFactory(),
            )
        }

        exception.message shouldContain "requires baseUrl"
    }

    @Test
    fun `rejects OpenAI-compatible model without api key`() {
        val exception = shouldThrow<IllegalStateException> {
            OpenAiCompatibleAiModelClientProvider(
                chatModelRegistry = chatModelRegistry(openAiModel(apiKey = null)),
                chatProperties = ChatProperties(),
                openAiCompatibleChatApiFactory = FakeOpenAiCompatibleChatApiFactory(),
            )
        }

        exception.message shouldContain "requires apiKey"
    }

    private fun chatModelRegistry(vararg models: ConfiguredChatModelProperties): ChatModelRegistry =
        ChatModelRegistry(ConfiguredChatModelsProperties(chatModels = models.toList()))

    private fun openAiModel(
        id: String = "cloud-gpt",
        baseUrl: String? = "https://api.example.com/v1",
        modelName: String? = "gpt-4.1-mini",
        apiKey: String? = "test-api-key",
    ): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            displayName = "Cloud GPT",
            runtime = ChatModelRuntime.OPENAI_COMPATIBLE,
            mode = ChatModelMode.ONLINE,
            availableOffline = false,
            baseUrl = baseUrl,
            modelName = modelName,
            apiKey = apiKey,
        )

    private fun ollamaModel(id: String): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            displayName = "Local Llama",
            runtime = ChatModelRuntime.OLLAMA,
            mode = ChatModelMode.LOCAL_SERVER,
            availableOffline = false,
            baseUrl = "http://localhost:11434",
            modelName = "llama3.2",
        )

    private class FakeOpenAiCompatibleChatApiFactory : OpenAiCompatibleChatApiFactory {
        val createdClients = mutableListOf<CreatedOpenAiCompatibleClient>()

        override fun create(
            baseUrl: String,
            apiKey: String,
            timeout: Duration,
        ): OpenAiCompatibleChatApi {
            createdClients += CreatedOpenAiCompatibleClient(
                baseUrl = baseUrl,
                apiKey = apiKey,
                timeout = timeout,
            )
            return FakeOpenAiCompatibleChatApi()
        }
    }

    private class FakeOpenAiCompatibleChatApi : OpenAiCompatibleChatApi {
        override fun chat(request: OpenAiCompatibleChatRequest): OpenAiCompatibleChatResponse =
            OpenAiCompatibleChatResponse(
                choices = listOf(
                    OpenAiCompatibleChatChoice(
                        message = OpenAiCompatibleChatMessage(
                            role = "assistant",
                            content = "Fake cloud answer",
                        ),
                    ),
                ),
            )
    }

    private data class CreatedOpenAiCompatibleClient(
        val baseUrl: String,
        val apiKey: String,
        val timeout: Duration,
    )
}
