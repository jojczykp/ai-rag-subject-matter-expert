package org.alterbit.aisme.chat

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test

class AiModelClientTest {
    @Test
    fun `defines provider-neutral chat contract`() {
        val client = EchoAiModelClient()
        val request = AiModelChatRequest(
            modelId = "local-llama",
            message = "How should I cook rice?",
            contextChunks = listOf(
                AiModelContextChunk(
                    content = "Use two parts water for one part rice.",
                    resourcePath = "subject-documents/culinary_expert/rice.txt",
                    chunkIndex = 0,
                ),
            ),
            timeout = Duration.ofSeconds(60),
        )

        val response = client.chat(request)

        client.modelId shouldBe "local-llama"
        response.answer shouldBe "local-llama: How should I cook rice? (1 chunks)"
        response.modelId shouldBe "local-llama"
    }

    private class EchoAiModelClient : AiModelClient {
        override val modelId: String = "local-llama"

        override fun chat(request: AiModelChatRequest): AiModelChatResponse =
            AiModelChatResponse(
                answer = "${request.modelId}: ${request.message} (${request.contextChunks.size} chunks)",
                modelId = request.modelId,
            )
    }
}
