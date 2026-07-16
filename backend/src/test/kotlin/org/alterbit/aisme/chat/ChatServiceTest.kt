package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.concurrent.CancellationException
import org.alterbit.aisme.chat.api.ChatRequestDto
import org.alterbit.aisme.chat.catalog.ChatModelAvailability
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityChecker
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityService
import org.alterbit.aisme.chat.catalog.ChatModelDescriptor
import org.alterbit.aisme.chat.catalog.ChatModelNotFoundException
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.alterbit.aisme.chat.catalog.ChatModelUnavailableException
import org.alterbit.aisme.chat.catalog.ChatModelProperties
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeProperties
import org.alterbit.aisme.chat.catalog.ChatModelsProperties
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeConfigProperties
import org.alterbit.aisme.chat.catalog.ChatProperties
import org.alterbit.aisme.document.SubjectDescriptor
import org.alterbit.aisme.document.SubjectRegistry
import org.alterbit.aisme.testsupport.culinarySubject
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException

class ChatServiceTest {
    @Test
    fun `delegates chat request to matching configured model client`() {
        val localModelClient = FakeChatModelClient(modelId = "local-ollama-llama")
        val cloudModelClient = FakeChatModelClient(modelId = "cloud-gpt")
        val contextChunks = contextChunks()
        val chatContextRetriever = FakeChatContextRetriever(contextChunks)
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(apiTimeout = Duration.ofSeconds(45)),
            chatContextRetriever = chatContextRetriever,
            chatModelClients = chatModelClients(localModelClient, cloudModelClient),
            subjectRegistry = subjectRegistry(),
        )

        val response = service.chat(
            ChatRequestDto(
                subjectId = "culinary-expert",
                modelId = "local-ollama-llama",
                message = "How should I cook rice?",
            ),
        )

        response.modelId shouldBe "local-ollama-llama"
        response.answer shouldBe "Fake answer for: How should I cook rice?"
        localModelClient.requests shouldContainExactly listOf(
            ChatModelRequest(
                modelId = "local-ollama-llama",
                message = "How should I cook rice?",
                contextChunks = contextChunks,
                apiTimeout = Duration.ofSeconds(45),
            ),
        )
        chatContextRetriever.messages shouldContainExactly listOf("How should I cook rice?")
        chatContextRetriever.subjectIds shouldContainExactly listOf("culinary-expert")
        chatContextRetriever.embeddingModelIds shouldContainExactly listOf(null)
        cloudModelClient.requests shouldContainExactly emptyList()
    }

    @Test
    fun `passes selected embedding model id to context retrieval`() {
        val modelClient = FakeChatModelClient(modelId = "local-ollama-llama")
        val chatContextRetriever = FakeChatContextRetriever(contextChunks())
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = chatContextRetriever,
            chatModelClients = chatModelClients(modelClient),
            subjectRegistry = subjectRegistry(),
        )

        service.chat(
            ChatRequestDto(
                subjectId = "culinary-expert",
                modelId = "local-ollama-llama",
                embeddingModelId = "local-bge-small",
                message = "How should I cook rice?",
            ),
        )

        chatContextRetriever.messages shouldContainExactly listOf("How should I cook rice?")
        chatContextRetriever.embeddingModelIds shouldContainExactly listOf("local-bge-small")
    }

    @Test
    fun `rejects unknown chat model id`() {
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            chatModelClients = chatModelClients(FakeChatModelClient(modelId = "local-ollama-llama")),
            subjectRegistry = subjectRegistry(),
        )

        val exception = shouldThrow<ChatModelNotFoundException> {
            service.chat(
                ChatRequestDto(
                    subjectId = "culinary-expert",
                    modelId = "missing-model",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception.message shouldContain "missing-model"
    }

    @Test
    fun `rejects configured chat model without matching model client`() {
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            chatModelClients = chatModelClients(FakeChatModelClient(modelId = "other-model")),
            subjectRegistry = subjectRegistry(),
        )

        val exception = shouldThrow<ChatModelClientNotFoundException> {
            service.chat(
                ChatRequestDto(
                    subjectId = "culinary-expert",
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
            ChatService(
                chatModelRegistry = chatModelRegistry(),
                chatModelAvailabilityService = chatModelAvailabilityService(),
                chatProperties = ChatProperties(),
                chatContextRetriever = FakeChatContextRetriever(),
                chatModelClients = chatModelClients(
                    FakeChatModelClient(modelId = "local-ollama-llama"),
                    FakeChatModelClient(modelId = "local-ollama-llama"),
                ),
                subjectRegistry = subjectRegistry(),
            )
        }

        exception.message shouldContain "duplicate"
    }

    @Test
    fun `rejects unavailable model before calling model client`() {
        val modelClient = FakeChatModelClient(modelId = "local-ollama-llama")
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(ChatModelAvailability.UNAVAILABLE),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            chatModelClients = chatModelClients(modelClient),
            subjectRegistry = subjectRegistry(),
        )

        val exception = shouldThrow<ChatModelUnavailableException> {
            service.chat(
                ChatRequestDto(
                    subjectId = "culinary-expert",
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
        val modelClient = FakeChatModelClient(modelId = "local-ollama-llama")
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(ChatModelAvailability.MISCONFIGURED),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            chatModelClients = chatModelClients(modelClient),
            subjectRegistry = subjectRegistry(),
        )

        val exception = shouldThrow<ChatModelUnavailableException> {
            service.chat(
                ChatRequestDto(
                    subjectId = "culinary-expert",
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
        val modelClient = FailingChatModelClient(modelId = "local-ollama-llama")
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            chatModelClients = chatModelClients(modelClient),
            subjectRegistry = subjectRegistry(),
        )

        val exception = shouldThrow<ChatModelProviderException> {
            service.chat(
                ChatRequestDto(
                    subjectId = "culinary-expert",
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
    fun `maps model client apiTimeout to provider apiTimeout`() {
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            chatModelClients = chatModelClients(
                FailingChatModelClient(
                    modelId = "local-ollama-llama",
                    failure = ResourceAccessException("Read timed out", SocketTimeoutException("Read timed out")),
                ),
            ),
            subjectRegistry = subjectRegistry(),
        )

        val exception = shouldThrow<ChatModelProviderTimeoutException> {
            service.chat(
                ChatRequestDto(
                    subjectId = "culinary-expert",
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
        val providerException = ChatModelProviderException(
            modelId = "local-ollama-llama",
            provider = "Custom provider",
            message = "custom provider failure",
        )
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            chatModelClients = chatModelClients(
                FailingChatModelClient(
                    modelId = "local-ollama-llama",
                    failure = providerException,
                ),
            ),
            subjectRegistry = subjectRegistry(),
        )

        val exception = shouldThrow<ChatModelProviderException> {
            service.chat(
                ChatRequestDto(
                    subjectId = "culinary-expert",
                    modelId = "local-ollama-llama",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception shouldBe providerException
    }

    @Test
    fun `does not map request cancellation`() {
        val service = ChatService(
            chatModelRegistry = chatModelRegistry(),
            chatModelAvailabilityService = chatModelAvailabilityService(),
            chatProperties = ChatProperties(),
            chatContextRetriever = FakeChatContextRetriever(),
            chatModelClients = chatModelClients(
                FailingChatModelClient(
                    modelId = "local-ollama-llama",
                    failure = CancellationException("request cancelled"),
                ),
            ),
            subjectRegistry = subjectRegistry(),
        )

        val exception = shouldThrow<CancellationException> {
            service.chat(
                ChatRequestDto(
                    subjectId = "culinary-expert",
                    modelId = "local-ollama-llama",
                    message = "How should I cook rice?",
                ),
            )
        }

        exception.message shouldBe "request cancelled"
    }

    private fun chatModelRegistry(): ChatModelRegistry =
        ChatModelRegistry(
            ChatModelsProperties(
                chatRuntimesById = mapOf(
                    "local-ollama" to ChatModelRuntimeConfigProperties(
                        type = ChatModelRuntime.OLLAMA,
                        baseUrl = "http://localhost:11434",
                    ),
                    "spring-ai" to ChatModelRuntimeConfigProperties(type = ChatModelRuntime.SPRING_AI),
                ),
                chatModelsById = mapOf(
                    "local-ollama-llama" to ChatModelProperties(
                        enabled = true,
                        displayName = "Local Ollama Llama",
                        runtime = ChatModelRuntimeProperties(
                            id = "local-ollama",
                            modelName = "llama3.2",
                        ),
                    ),
                    "cloud-gpt" to ChatModelProperties(
                        enabled = true,
                        displayName = "Cloud GPT",
                        runtime = ChatModelRuntimeProperties(id = "spring-ai"),
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

                    override fun check(model: ChatModelDescriptor, apiTimeout: Duration): ChatModelAvailability =
                        availability
                },
            ),
            clock = java.time.Clock.systemUTC(),
        )

    private fun chatModelClients(vararg clients: ChatModelClient): ChatModelClients =
        ChatModelClients(listOf(ChatModelClientProvider { clients.toList() }))

    private fun subjectRegistry(): SubjectRegistry =
        object : SubjectRegistry {
            override fun subjects(): List<SubjectDescriptor> =
                listOf(culinarySubject())

            override fun getByIdOrThrow(subjectId: String): SubjectDescriptor =
                subjects().first { subject -> subject.id == subjectId }
        }

    private fun contextChunks(): List<ChatModelContextChunk> =
        listOf(
            ChatModelContextChunk(
                content = "Use two parts water for one part rice.",
                resourcePath = "culinary_expert/rice.txt",
                chunkIndex = 0,
            ),
        )

    private class FakeChatContextRetriever(
        private val chunks: List<ChatModelContextChunk> = emptyList(),
    ) : ChatContextRetriever {
        val subjectIds = mutableListOf<String>()
        val messages = mutableListOf<String>()
        val embeddingModelIds = mutableListOf<String?>()

        override fun retrieve(
            subjectId: String,
            message: String,
            embeddingModelId: String?,
        ): List<ChatModelContextChunk> {
            subjectIds += subjectId
            messages += message
            embeddingModelIds += embeddingModelId
            return chunks
        }
    }

    private class FailingChatModelClient(
        override val modelId: String,
        private val failure: RuntimeException = IllegalStateException("model call failed"),
    ) : ChatModelClient {
        var callCount = 0

        override fun chat(request: ChatModelRequest): ChatModelResponse {
            callCount += 1
            throw failure
        }
    }
}
