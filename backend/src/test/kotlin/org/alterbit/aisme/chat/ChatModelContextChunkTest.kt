package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ChatModelContextChunkTest {
    @Test
    fun `creates model context chunk`() {
        val chunk = ChatModelContextChunk(
            content = "Use two parts water for one part rice.",
            resourcePath = "subject_documents/culinary_expert/rice.txt",
            chunkIndex = 0,
        )

        chunk.content shouldBe "Use two parts water for one part rice."
        chunk.resourcePath shouldBe "subject_documents/culinary_expert/rice.txt"
        chunk.chunkIndex shouldBe 0
    }

    @Test
    fun `rejects blank content`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelContextChunk(
                content = " ",
                resourcePath = "subject_documents/culinary_expert/rice.txt",
                chunkIndex = 0,
            )
        }

        exception.message shouldContain "content"
    }

    @Test
    fun `rejects blank resource path`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelContextChunk(
                content = "Use two parts water for one part rice.",
                resourcePath = " ",
                chunkIndex = 0,
            )
        }

        exception.message shouldContain "resourcePath"
    }

    @Test
    fun `rejects negative chunk index`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelContextChunk(
                content = "Use two parts water for one part rice.",
                resourcePath = "subject_documents/culinary_expert/rice.txt",
                chunkIndex = -1,
            )
        }

        exception.message shouldContain "chunkIndex"
    }
}
