package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chatmodel.ChatModelAvailability
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityChecker
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityProperties
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityService
import org.alterbit.aisme.chatmodel.ChatModelDescriptor
import org.alterbit.aisme.chatmodel.ChatModelMode
import org.alterbit.aisme.chatmodel.ChatModelNotFoundException
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.alterbit.aisme.chatmodel.ChatModelUnavailableException
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
            chatModelAvailabilityService = chatModelAvailabilityService(),
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
            chatModelAvailabilityService = chatModelAvailabilityService(),
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
            chatModelAvailabilityService = chatModelAvailabilityService(),
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
                chatModelAvailabilityService = chatModelAvailabilityService(),
                chatProperties = ChatProperties(),
                aiModelClients = listOf(
                    FakeAiModelClient(modelId = "local-ollama-llama"),
                    FakeAiModelClient(modelId = "local-ollama-llama"),
                ),
            )
        }

        exception.message shouldContain "duplicate"
    }

    @Test
    fun `rejects unavailable model before calling model client`() {
        val modelClient = FakeAiModelClient(modelId = "local-ollama-llama")
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(ChatModelAvailability.UNAVAILABLE),
            chatProperties = ChatProperties(),
            aiModelClients = listOf(modelClient),
        )

        val exception = shouldThrow<ChatModelUnavailableException> {
            service.chat(
                ChatRequestDto(
                    modelId = "local-ollama-llama",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception.modelId shouldBe "local-ollama-llama"
        exception.availability shouldBe ChatModelAvailability.UNAVAILABLE
        modelClient.requests shouldContainExactly emptyList()
    }

    @Test
    fun `does not retry chat generation automatically`() {
        val modelClient = FailingAiModelClient(modelId = "local-ollama-llama")
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            aiModelClients = listOf(modelClient),
        )

        val exception = shouldThrow<IllegalStateException> {
            service.chat(
                ChatRequestDto(
                    modelId = "local-ollama-llama",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception.message shouldBe "model call failed"
        modelClient.callCount shouldBe 1
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

    private fun chatModelAvailabilityService(
        availability: ChatModelAvailability = ChatModelAvailability.AVAILABLE,
    ): ChatModelAvailabilityService =
        ChatModelAvailabilityService(
            properties = ChatModelAvailabilityProperties(timeout = Duration.ofSeconds(5)),
            checkers = listOf(
                object : ChatModelAvailabilityChecker {
                    override fun supports(model: ChatModelDescriptor): Boolean =
                        model.id == "local-ollama-llama"

                    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
                        availability
                },
            ),
            clock = java.time.Clock.systemUTC(),
        )

    private class FailingAiModelClient(
        override val modelId: String,
    ) : AiModelClient {
        var callCount = 0

        override fun chat(request: AiModelChatRequest): AiModelChatResponse {
            callCount += 1
            throw IllegalStateException("model call failed")
        }
    }
}
