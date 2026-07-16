package org.alterbit.aisme.chat

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test

class ChatModelClientTest {
    @Test
    fun `defines provider-neutral chat contract`() {
        val client = EchoChatModelClient()
        val request = ChatModelRequest(
            modelId = "local-llama",
            message = "How should I cook rice?",
            contextChunks = listOf(
                ChatModelContextChunk(
                    content = "Use two parts water for one part rice.",
                    resourcePath = "subject_documents/culinary_expert/rice.txt",
                    chunkIndex = 0,
                ),
            ),
            apiTimeout = Duration.ofSeconds(60),
        )

        val response = client.chat(request)

        client.modelId shouldBe "local-llama"
        response.answer shouldBe "local-llama: How should I cook rice? (1 chunks)"
        response.modelId shouldBe "local-llama"
    }

    private class EchoChatModelClient : ChatModelClient {
        override val modelId: String = "local-llama"

        override fun chat(request: ChatModelRequest): ChatModelResponse =
            ChatModelResponse(
                answer = "${request.modelId}: ${request.message} (${request.contextChunks.size} chunks)",
                modelId = request.modelId,
            )
    }
}
