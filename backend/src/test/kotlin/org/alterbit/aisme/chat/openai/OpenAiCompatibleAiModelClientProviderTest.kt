package org.alterbit.aisme.chat.openai

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelProperties
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelsProperties
import org.alterbit.aisme.modelcatalog.ConfiguredChatRuntimeProperties
import org.junit.jupiter.api.Test

class OpenAiCompatibleAiModelClientProviderTest {
    @Test
    fun `creates one client per configured OpenAI-compatible model`() {
        val factory = FakeOpenAiCompatibleChatApiFactory()
        val provider = OpenAiCompatibleAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                openAiModel(id = "cloud-gpt", modelName = "gpt-4.1-mini"),
                ollamaModel(id = "local-llama"),
                openAiModel(id = "cloud-qwen", runtimeId = "openai-compatible-alt", modelName = "qwen-plus"),
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
        val exception = shouldThrow<IllegalArgumentException> {
            OpenAiCompatibleAiModelClientProvider(
                chatModelRegistry = ChatModelRegistry(
                    ConfiguredChatModelsProperties(
                        runtimes = mapOf(
                            "openai-compatible" to ConfiguredChatRuntimeProperties(
                                type = ChatModelRuntime.OPENAI_COMPATIBLE,
                                apiKey = "test-api-key",
                            ),
                        ),
                        chatModelsById = mapOf(openAiModel()),
                    ),
                ),
                chatProperties = ChatProperties(),
                openAiCompatibleChatApiFactory = FakeOpenAiCompatibleChatApiFactory(),
            )
        }

        exception.message shouldContain "base-url"
        exception.message shouldContain "is required"
    }

    @Test
    fun `skips OpenAI-compatible model without api key`() {
        val factory = FakeOpenAiCompatibleChatApiFactory()
        val provider = OpenAiCompatibleAiModelClientProvider(
            chatModelRegistry = chatModelRegistry(
                openAiModel(id = "missing-api-key", apiKey = null),
                openAiModel(id = "configured-api-key"),
            ),
            chatProperties = ChatProperties(),
            openAiCompatibleChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("configured-api-key")
        factory.createdClients.map { it.apiKey } shouldContainExactly listOf("test-api-key")
    }

    private fun chatModelRegistry(vararg models: Pair<String, ConfiguredChatModelProperties>): ChatModelRegistry =
        ChatModelRegistry(
            ConfiguredChatModelsProperties(
                runtimes = mapOf(
                    "openai-compatible" to ConfiguredChatRuntimeProperties(
                        type = ChatModelRuntime.OPENAI_COMPATIBLE,
                        baseUrl = "https://api.example.com/v1",
                        apiKey = "test-api-key",
                    ),
                    "openai-compatible-alt" to ConfiguredChatRuntimeProperties(
                        type = ChatModelRuntime.OPENAI_COMPATIBLE,
                        baseUrl = "https://gateway.example.com/v1",
                        apiKey = "test-api-key",
                    ),
                    "openai-compatible-no-key" to ConfiguredChatRuntimeProperties(
                        type = ChatModelRuntime.OPENAI_COMPATIBLE,
                        baseUrl = "https://no-key.example.com/v1",
                    ),
                    "local-ollama" to ConfiguredChatRuntimeProperties(
                        type = ChatModelRuntime.OLLAMA,
                        baseUrl = "http://localhost:11434",
                    ),
                ),
                chatModelsById = models.toMap(),
            ),
        )

    private fun openAiModel(
        id: String = "cloud-gpt",
        runtimeId: String? = null,
        modelName: String? = "gpt-4.1-mini",
        apiKey: String? = "test-api-key",
    ): Pair<String, ConfiguredChatModelProperties> =
        id to ConfiguredChatModelProperties(
            enabled = true,
            displayName = "Cloud GPT",
            runtimeId = runtimeId ?: if (apiKey == null) "openai-compatible-no-key" else "openai-compatible",
            modelName = modelName,
        )

    private fun ollamaModel(id: String): Pair<String, ConfiguredChatModelProperties> =
        id to ConfiguredChatModelProperties(
            enabled = true,
            displayName = "Local Llama",
            runtimeId = "local-ollama",
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
