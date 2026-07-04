package org.alterbit.aisme.chat.ollama

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import java.time.Instant
import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelContextChunk
import org.alterbit.aisme.chatmodel.ChatModelDescriptor
import org.alterbit.aisme.chatmodel.ChatModelMode
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.alterbit.aisme.chatmodel.chatModel
import org.junit.jupiter.api.Test
import org.springframework.ai.ollama.api.OllamaApi

class OllamaAiModelClientTest {
    @Test
    fun `exposes configured model id`() {
        val client = OllamaAiModelClient(
            model = ollamaModel(id = "local-llama"),
            chatApi = FakeOllamaChatApi(),
        )

        client.modelId shouldBe "local-llama"
    }

    @Test
    fun `sends chat request to configured ollama model`() {
        val chatApi = FakeOllamaChatApi(answer = " Use two parts water. ")
        val client = OllamaAiModelClient(
            model = ollamaModel(id = "local-llama", modelName = "llama3.2"),
            chatApi = chatApi,
        )

        val response = client.chat(
            AiModelChatRequest(
                modelId = "local-llama",
                message = "How should I cook rice?",
                contextChunks = listOf(
                    AiModelContextChunk(
                        content = "Use two parts water for one part rice.",
                        resourcePath = "subject-documents/culinary_expert/rice.txt",
                        chunkIndex = 0,
                    ),
                ),
                timeout = Duration.ofSeconds(45),
            ),
        )

        response.modelId shouldBe "local-llama"
        response.answer shouldBe "Use two parts water."
        chatApi.requests.single().model() shouldBe "llama3.2"
        chatApi.requests.single().stream() shouldBe false
        chatApi.requests.single().messages().single().role() shouldBe OllamaApi.Message.Role.USER
        chatApi.requests.single().messages().single().content() shouldBe """
            Context:
            Use two parts water for one part rice.

            Question:
            How should I cook rice?
        """.trimIndent()
    }

    @Test
    fun `rejects request for another model id`() {
        val client = OllamaAiModelClient(
            model = ollamaModel(id = "local-llama"),
            chatApi = FakeOllamaChatApi(),
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.chat(request(modelId = "local-qwen"))
        }

        exception.message shouldContain "cannot handle"
    }

    @Test
    fun `rejects ollama model without model name`() {
        val exception = shouldThrow<IllegalStateException> {
            OllamaAiModelClient(
                model = ollamaModel(modelName = null),
                chatApi = FakeOllamaChatApi(),
            )
        }

        exception.message shouldContain "requires modelName"
    }

    private fun request(modelId: String = "local-llama"): AiModelChatRequest =
        AiModelChatRequest(
            modelId = modelId,
            message = "How should I cook rice?",
            contextChunks = emptyList(),
            timeout = Duration.ofSeconds(60),
        )

    private fun ollamaModel(
        id: String = "local-llama",
        modelName: String? = "llama3.2",
    ): ChatModelDescriptor =
        chatModel(
            id = id,
            runtime = ChatModelRuntime.OLLAMA,
            mode = ChatModelMode.LOCAL_SERVER,
            availableOffline = false,
            baseUrl = "http://localhost:11434",
            modelName = modelName,
        )

    private class FakeOllamaChatApi(
        private val answer: String = "Fake Ollama answer",
    ) : OllamaChatApi {
        val requests = mutableListOf<OllamaApi.ChatRequest>()

        override fun chat(request: OllamaApi.ChatRequest): OllamaApi.ChatResponse {
            requests += request
            return OllamaApi.ChatResponse(
                request.model(),
                Instant.EPOCH,
                OllamaApi.Message.builder(OllamaApi.Message.Role.ASSISTANT)
                    .content(answer)
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
        }

        override fun modelNames(): Set<String> =
            emptySet()
    }
}
