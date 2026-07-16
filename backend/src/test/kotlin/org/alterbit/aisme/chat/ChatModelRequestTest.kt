package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.junit.jupiter.api.Test

class ChatModelRequestTest {
    @Test
    fun `creates provider-neutral model request`() {
        val contextChunk = ChatModelContextChunk(
            content = "Use two parts water for one part rice.",
            resourcePath = "subject-documents/culinary_expert/rice.txt",
            chunkIndex = 0,
        )

        val request = ChatModelRequest(
            modelId = "local-llama",
            message = "How should I cook rice?",
            contextChunks = listOf(contextChunk),
            apiTimeout = Duration.ofSeconds(60),
        )

        request.modelId shouldBe "local-llama"
        request.message shouldBe "How should I cook rice?"
        request.contextChunks shouldContainExactly listOf(contextChunk)
        request.apiTimeout shouldBe Duration.ofSeconds(60)
    }

    @Test
    fun `rejects blank model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRequest(
                modelId = " ",
                message = "How should I cook rice?",
                contextChunks = emptyList(),
                apiTimeout = Duration.ofSeconds(60),
            )
        }

        exception.message shouldContain "modelId"
    }

    @Test
    fun `rejects blank message`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRequest(
                modelId = "local-llama",
                message = " ",
                contextChunks = emptyList(),
                apiTimeout = Duration.ofSeconds(60),
            )
        }

        exception.message shouldContain "message"
    }

    @Test
    fun `rejects zero apiTimeout`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRequest(
                modelId = "local-llama",
                message = "How should I cook rice?",
                contextChunks = emptyList(),
                apiTimeout = Duration.ZERO,
            )
        }

        exception.message shouldContain "apiTimeout"
    }
}
