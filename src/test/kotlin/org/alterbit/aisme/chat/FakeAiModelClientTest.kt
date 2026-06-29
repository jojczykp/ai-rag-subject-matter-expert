package org.alterbit.aisme.chat

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FakeAiModelClientTest {
    @Test
    fun `returns deterministic default response`() {
        val client = FakeAiModelClient()

        val response = client.chat(
            AiModelChatRequest(
                modelId = "local-llama",
                message = "How should I cook rice?",
                contextChunks = emptyList(),
            ),
        )

        client.modelId shouldBe "local-ollama-llama"
        response.answer shouldBe "Fake answer for: How should I cook rice?"
        response.modelId shouldBe "local-llama"
    }

    @Test
    fun `records requests in order`() {
        val client = FakeAiModelClient()
        val firstRequest = request(message = "First question")
        val secondRequest = request(message = "Second question")

        client.chat(firstRequest)
        client.chat(secondRequest)

        client.requests shouldContainExactly listOf(firstRequest, secondRequest)
    }

    @Test
    fun `uses custom response factory`() {
        val client = FakeAiModelClient(
            modelId = "local-llama",
        ) { request ->
            AiModelChatResponse(
                answer = "Context chunks: ${request.contextChunks.size}",
                modelId = request.modelId,
            )
        }

        val response = client.chat(
            request(
                contextChunks = listOf(
                    AiModelContextChunk(
                        content = "Use two parts water for one part rice.",
                        resourcePath = "subject-documents/culinary_expert/rice.txt",
                        chunkIndex = 0,
                    ),
                ),
            ),
        )

        response.answer shouldBe "Context chunks: 1"
        response.modelId shouldBe "local-llama"
    }

    private fun request(
        message: String = "How should I cook rice?",
        contextChunks: List<AiModelContextChunk> = emptyList(),
    ): AiModelChatRequest =
        AiModelChatRequest(
            modelId = "local-llama",
            message = message,
            contextChunks = contextChunks,
        )
}
