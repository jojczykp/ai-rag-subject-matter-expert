package org.alterbit.aisme.chat.runtime.ollama

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import java.time.Instant
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeConfigProperties
import org.alterbit.aisme.chat.catalog.ChatModelProperties
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeProperties
import org.alterbit.aisme.chat.catalog.ChatModelsProperties
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
                ollamaModel(id = "local-qwen", runtimeId = "local-ollama-alt", modelName = "qwen2.5"),
            ),
            chatProperties = ChatProperties(apiTimeout = Duration.ofSeconds(30)),
            ollamaChatApiFactory = factory,
        )

        provider.clients().map { it.modelId } shouldContainExactly listOf("local-llama", "local-qwen")
        factory.createdClients shouldContainExactly listOf(
            CreatedOllamaClient(
                baseUrl = "http://localhost:11434",
                apiTimeout = Duration.ofSeconds(30),
            ),
            CreatedOllamaClient(
                baseUrl = "http://localhost:11435",
                apiTimeout = Duration.ofSeconds(30),
            ),
        )
    }

    @Test
    fun `rejects ollama model without base url`() {
        val exception = shouldThrow<IllegalArgumentException> {
            OllamaAiModelClientProvider(
                chatModelRegistry = ChatModelRegistry(
                    ChatModelsProperties(
                        chatRuntimesById = mapOf(
                            "local-ollama" to ChatModelRuntimeConfigProperties(type = ChatModelRuntime.OLLAMA),
                        ),
                        chatModelsById = mapOf(ollamaModel()),
                    ),
                ),
                chatProperties = ChatProperties(),
                ollamaChatApiFactory = FakeOllamaChatApiFactory(),
            )
        }

        exception.message shouldContain "base-url"
        exception.message shouldContain "is required"
    }

    private fun chatModelRegistry(vararg models: Pair<String, ChatModelProperties>): ChatModelRegistry =
        ChatModelRegistry(
            ChatModelsProperties(
                chatRuntimesById = mapOf(
                    "local-ollama" to ChatModelRuntimeConfigProperties(
                        type = ChatModelRuntime.OLLAMA,
                        baseUrl = "http://localhost:11434",
                    ),
                    "local-ollama-alt" to ChatModelRuntimeConfigProperties(
                        type = ChatModelRuntime.OLLAMA,
                        baseUrl = "http://localhost:11435",
                    ),
                    "spring-ai" to ChatModelRuntimeConfigProperties(type = ChatModelRuntime.SPRING_AI),
                ),
                chatModelsById = models.toMap(),
            ),
        )

    private fun ollamaModel(
        id: String = "local-llama",
        runtimeId: String? = "local-ollama",
        modelName: String? = "llama3.2",
    ): Pair<String, ChatModelProperties> =
        id to ChatModelProperties(
            enabled = true,
            displayName = "Local Llama",
            runtime = ChatModelRuntimeProperties(
                id = runtimeId,
                modelName = modelName,
            ),
        )

    private fun springAiModel(id: String): Pair<String, ChatModelProperties> =
        id to ChatModelProperties(
            enabled = true,
            displayName = "Cloud GPT",
            runtime = ChatModelRuntimeProperties(id = "spring-ai"),
        )

    private class FakeOllamaChatApiFactory : OllamaChatApiFactory {
        val createdClients = mutableListOf<CreatedOllamaClient>()

        override fun create(baseUrl: String, apiTimeout: Duration): OllamaChatApi {
            createdClients += CreatedOllamaClient(baseUrl = baseUrl, apiTimeout = apiTimeout)
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
        val apiTimeout: Duration,
    )
}
