package org.alterbit.aisme.chat.runtime.embedded

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelContextChunk
import org.alterbit.aisme.chat.catalog.ChatModelMode
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.alterbit.aisme.chat.catalog.chatModel
import org.junit.jupiter.api.Test

class EmbeddedLlamaAiModelClientTest {
    @Test
    fun `exposes configured model id`() {
        val client = EmbeddedLlamaAiModelClient(
            model = embeddedModel(id = "embedded-qwen-0-5b"),
            chatApi = FakeLlamaServerChatApi(),
        )

        client.modelId shouldBe "embedded-qwen-0-5b"
    }

    @Test
    fun `sends chat request to configured embedded llama model`() {
        val chatApi = FakeLlamaServerChatApi(answer = " Use two parts water. ")
        val client = EmbeddedLlamaAiModelClient(
            model = embeddedModel(id = "embedded-qwen-0-5b"),
            chatApi = chatApi,
        )

        val response = client.chat(
            AiModelChatRequest(
                modelId = "embedded-qwen-0-5b",
                message = "How should I cook rice?",
                contextChunks = listOf(
                    AiModelContextChunk(
                        content = "Use two parts water for one part rice.",
                        resourcePath = "subject-documents/culinary_expert/rice.txt",
                        chunkIndex = 0,
                    ),
                ),
                apiTimeout = Duration.ofSeconds(45),
            ),
        )

        response.modelId shouldBe "embedded-qwen-0-5b"
        response.answer shouldBe "Use two parts water."
        chatApi.requests.single().stream shouldBe false
        chatApi.requests.single().prompt shouldBe """
            Context:
            Use two parts water for one part rice.

            Question:
            How should I cook rice?
        """.trimIndent()
    }

    @Test
    fun `rejects request for another model id`() {
        val client = EmbeddedLlamaAiModelClient(
            model = embeddedModel(id = "embedded-qwen-0-5b"),
            chatApi = FakeLlamaServerChatApi(),
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.chat(request(modelId = "other-embedded-model"))
        }

        exception.message shouldContain "cannot handle"
    }

    @Test
    fun `rejects blank provider response`() {
        val client = EmbeddedLlamaAiModelClient(
            model = embeddedModel(id = "embedded-qwen-0-5b"),
            chatApi = FakeLlamaServerChatApi(answer = " "),
        )

        val exception = shouldThrow<IllegalStateException> {
            client.chat(request())
        }

        exception.message shouldContain "blank answer"
    }

    private fun request(modelId: String = "embedded-qwen-0-5b"): AiModelChatRequest =
        AiModelChatRequest(
            modelId = modelId,
            message = "How should I cook rice?",
            contextChunks = emptyList(),
            apiTimeout = Duration.ofSeconds(60),
        )

    private fun embeddedModel(id: String) = chatModel(
        id = id,
        displayName = "Embedded Qwen",
        runtime = ChatModelRuntime.EMBEDDED_LLAMA,
        mode = ChatModelMode.EMBEDDED_OFFLINE,
        availableOffline = true,
        baseUrl = null,
        modelName = null,
    )

    private class FakeLlamaServerChatApi(
        private val answer: String = "Fake embedded answer",
    ) : LlamaServerChatApi {
        val requests = mutableListOf<LlamaServerCompletionRequest>()

        override fun complete(request: LlamaServerCompletionRequest): LlamaServerCompletionResponse {
            requests += request
            return LlamaServerCompletionResponse(content = answer)
        }
    }
}
