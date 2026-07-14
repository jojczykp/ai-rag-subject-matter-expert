package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.concurrent.CancellationException
import org.alterbit.aisme.modelcatalog.ChatModelAvailability
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityChecker
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityService
import org.alterbit.aisme.modelcatalog.ChatModelDescriptor
import org.alterbit.aisme.modelcatalog.ChatModelNotFoundException
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.ChatModelUnavailableException
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelProperties
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelsProperties
import org.alterbit.aisme.modelcatalog.ConfiguredChatRuntimeProperties
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException

class AiChatServiceTest {
    @Test
    fun `delegates chat request to matching configured model client`() {
        val localModelClient = FakeAiModelClient(modelId = "local-ollama-llama")
        val cloudModelClient = FakeAiModelClient(modelId = "cloud-gpt")
        val contextChunks = contextChunks()
        val chatContextRetriever = FakeChatContextRetriever(contextChunks)
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(timeout = Duration.ofSeconds(45)),
            chatContextRetriever = chatContextRetriever,
            aiModelClients = aiModelClients(localModelClient, cloudModelClient),
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
                contextChunks = contextChunks,
                timeout = Duration.ofSeconds(45),
            ),
        )
        chatContextRetriever.messages shouldContainExactly listOf("How should I cook rice?")
        cloudModelClient.requests shouldContainExactly emptyList()
    }

    @Test
    fun `rejects unknown chat model id`() {
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            aiModelClients = aiModelClients(FakeAiModelClient(modelId = "local-ollama-llama")),
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
            chatContextRetriever = FakeChatContextRetriever(),
            aiModelClients = aiModelClients(FakeAiModelClient(modelId = "other-model")),
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
                chatContextRetriever = FakeChatContextRetriever(),
                aiModelClients = aiModelClients(
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
            chatContextRetriever = FakeChatContextRetriever(),
            aiModelClients = aiModelClients(modelClient),
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
    fun `rejects misconfigured model before calling model client`() {
        val modelClient = FakeAiModelClient(modelId = "local-ollama-llama")
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(ChatModelAvailability.MISCONFIGURED),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            aiModelClients = aiModelClients(modelClient),
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
        exception.availability shouldBe ChatModelAvailability.MISCONFIGURED
        modelClient.requests shouldContainExactly emptyList()
    }

    @Test
    fun `maps model client failure to provider error`() {
        val modelClient = FailingAiModelClient(modelId = "local-ollama-llama")
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            aiModelClients = aiModelClients(modelClient),
        )

        val exception = shouldThrow<AiModelProviderException> {
            service.chat(
                ChatRequestDto(
                    modelId = "local-ollama-llama",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception.modelId shouldBe "local-ollama-llama"
        exception.provider shouldBe "Ollama"
        modelClient.callCount shouldBe 1
    }

    @Test
    fun `maps model client timeout to provider timeout`() {
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            aiModelClients = aiModelClients(
                FailingAiModelClient(
                    modelId = "local-ollama-llama",
                    failure = ResourceAccessException("Read timed out", SocketTimeoutException("Read timed out")),
                ),
            ),
        )

        val exception = shouldThrow<AiModelProviderTimeoutException> {
            service.chat(
                ChatRequestDto(
                    modelId = "local-ollama-llama",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception.modelId shouldBe "local-ollama-llama"
        exception.provider shouldBe "Ollama"
    }

    @Test
    fun `does not remap provider exception from model client`() {
        val providerException = AiModelProviderException(
            modelId = "local-ollama-llama",
            provider = "Custom provider",
            message = "custom provider failure",
        )
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            aiModelClients = aiModelClients(
                FailingAiModelClient(
                    modelId = "local-ollama-llama",
                    failure = providerException,
                ),
            ),
        )

        val exception = shouldThrow<AiModelProviderException> {
            service.chat(
                ChatRequestDto(
                    modelId = "local-ollama-llama",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception shouldBe providerException
    }

    @Test
    fun `does not map request cancellation`() {
        val service = AiChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            aiModelClients = aiModelClients(
                FailingAiModelClient(
                    modelId = "local-ollama-llama",
                    failure = CancellationException("request cancelled"),
                ),
            ),
        )

        val exception = shouldThrow<CancellationException> {
            service.chat(
                ChatRequestDto(
                    modelId = "local-ollama-llama",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception.message shouldBe "request cancelled"
    }

    private fun chatModelRegistry(): ChatModelRegistry =
        ChatModelRegistry(
            ConfiguredChatModelsProperties(
                runtimes = mapOf(
                    "local-ollama" to ConfiguredChatRuntimeProperties(
                        type = ChatModelRuntime.OLLAMA,
                        baseUrl = "http://localhost:11434",
                    ),
                    "spring-ai" to ConfiguredChatRuntimeProperties(type = ChatModelRuntime.SPRING_AI),
                ),
                chatModels = listOf(
                    ConfiguredChatModelProperties(
                        id = "local-ollama-llama",
                        enabled = true,
                        displayName = "Local Ollama Llama",
                        runtimeId = "local-ollama",
                        modelName = "llama3.2",
                    ),
                    ConfiguredChatModelProperties(
                        id = "cloud-gpt",
                        enabled = true,
                        displayName = "Cloud GPT",
                        runtimeId = "spring-ai",
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

    private fun aiModelClients(vararg clients: AiModelClient): AiModelClients =
        AiModelClients(listOf(AiModelClientProvider { clients.toList() }))

    private fun contextChunks(): List<AiModelContextChunk> =
        listOf(
            AiModelContextChunk(
                content = "Use two parts water for one part rice.",
                resourcePath = "culinary_expert/rice.txt",
                chunkIndex = 0,
            ),
        )

    private class FakeChatContextRetriever(
        private val chunks: List<AiModelContextChunk> = emptyList(),
    ) : ChatContextRetriever {
        val messages = mutableListOf<String>()

        override fun retrieve(message: String): List<AiModelContextChunk> {
            messages += message
            return chunks
        }
    }

    private class FailingAiModelClient(
        override val modelId: String,
        private val failure: RuntimeException = IllegalStateException("model call failed"),
    ) : AiModelClient {
        var callCount = 0

        override fun chat(request: AiModelChatRequest): AiModelChatResponse {
            callCount += 1
            throw failure
        }
    }
}
