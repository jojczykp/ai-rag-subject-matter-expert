package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chatmodel.ChatModelMode
import org.alterbit.aisme.chatmodel.ChatModelNotFoundException
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.alterbit.aisme.chatmodel.ConfiguredChatModelProperties
import org.alterbit.aisme.chatmodel.ConfiguredChatModelsProperties
import org.junit.jupiter.api.Test

class AiChatServiceTest {
    @Test
    fun `delegates chat request to matching configured model client`() {
        val localModelClient = FakeAiModelClient(modelId = "local-ollama-llama")
        val cloudModelClient = FakeAiModelClient(modelId = "cloud-gpt")
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatProperties = ChatProperties(timeout = Duration.ofSeconds(45)),
            aiModelClients = listOf(localModelClient, cloudModelClient),
        )

        val response = service.chat(
            ChatRequestDto(
                modelId = "local-ollama-llama",
                message = "How should I cook rice?",
            ),
        )

        response.modelId shouldBe "local-ollama-llama"
        response.answer shouldBe "Fake answer for: How should I cook rice?"
        localModelClient.requests shouldContainExactly listOf(
            AiModelChatRequest(
                modelId = "local-ollama-llama",
                message = "How should I cook rice?",
                contextChunks = emptyList(),
                timeout = Duration.ofSeconds(45),
            ),
        )
        cloudModelClient.requests shouldContainExactly emptyList()
    }

    @Test
    fun `rejects unknown chat model id`() {
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatProperties = ChatProperties(),
            aiModelClients = listOf(FakeAiModelClient(modelId = "local-ollama-llama")),
        )

        val exception = shouldThrow<ChatModelNotFoundException> {
            service.chat(
                ChatRequestDto(
                    modelId = "missing-model",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception.message shouldContain "missing-model"
    }

    @Test
    fun `rejects configured chat model without matching model client`() {
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatProperties = ChatProperties(),
            aiModelClients = listOf(FakeAiModelClient(modelId = "other-model")),
        )

        val exception = shouldThrow<AiModelClientNotFoundException> {
            service.chat(
                ChatRequestDto(
                    modelId = "local-ollama-llama",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception.message shouldContain "local-ollama-llama"
    }

    @Test
    fun `rejects duplicate model clients`() {
        val exception = shouldThrow<IllegalArgumentException> {
            AiChatService(
                chatModelRegistry = chatModelRegistry(),
                chatProperties = ChatProperties(),
                aiModelClients = listOf(
                    FakeAiModelClient(modelId = "local-ollama-llama"),
                    FakeAiModelClient(modelId = "local-ollama-llama"),
                ),
            )
        }

        exception.message shouldContain "duplicate"
    }

    private fun chatModelRegistry(): ChatModelRegistry =
        ChatModelRegistry(
            ConfiguredChatModelsProperties(
                chatModels = listOf(
                    ConfiguredChatModelProperties(
                        id = "local-ollama-llama",
                        displayName = "Local Ollama Llama",
                        runtime = ChatModelRuntime.OLLAMA,
                        mode = ChatModelMode.LOCAL_SERVER,
                        availableOffline = false,
                        baseUrl = "http://localhost:11434",
                    ),
                    ConfiguredChatModelProperties(
                        id = "cloud-gpt",
                        displayName = "Cloud GPT",
                        runtime = ChatModelRuntime.SPRING_AI,
                        mode = ChatModelMode.ONLINE,
                        availableOffline = false,
                    ),
                ),
            ),
        )
}
