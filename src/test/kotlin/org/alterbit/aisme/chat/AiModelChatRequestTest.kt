package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class AiModelChatRequestTest {
    @Test
    fun `creates provider-neutral model request`() {
        val contextChunk = AiModelContextChunk(
            content = "Use two parts water for one part rice.",
            resourcePath = "subject-documents/culinary_expert/rice.txt",
            chunkIndex = 0,
        )

        val request = AiModelChatRequest(
            modelId = "local-llama",
            message = "How should I cook rice?",
            contextChunks = listOf(contextChunk),
        )

        request.modelId shouldBe "local-llama"
        request.message shouldBe "How should I cook rice?"
        request.contextChunks shouldContainExactly listOf(contextChunk)
    }

    @Test
    fun `rejects blank model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            AiModelChatRequest(
                modelId = " ",
                message = "How should I cook rice?",
                contextChunks = emptyList(),
            )
        }

        exception.message shouldContain "modelId"
    }

    @Test
    fun `rejects blank message`() {
        val exception = shouldThrow<IllegalArgumentException> {
            AiModelChatRequest(
                modelId = "local-llama",
                message = " ",
                contextChunks = emptyList(),
            )
        }

        exception.message shouldContain "message"
    }
}
