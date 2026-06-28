package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class AiModelContextChunkTest {
    @Test
    fun `creates model context chunk`() {
        AiModelContextChunk(
            content = "Use two parts water for one part rice.",
            resourcePath = "subject-documents/culinary_expert/rice.txt",
            chunkIndex = 0,
        ) shouldBe AiModelContextChunk(
            content = "Use two parts water for one part rice.",
            resourcePath = "subject-documents/culinary_expert/rice.txt",
            chunkIndex = 0,
        )
    }

    @Test
    fun `rejects blank content`() {
        val exception = shouldThrow<IllegalArgumentException> {
            AiModelContextChunk(
                content = " ",
                resourcePath = "subject-documents/culinary_expert/rice.txt",
                chunkIndex = 0,
            )
        }

        exception.message shouldContain "content"
    }

    @Test
    fun `rejects blank resource path`() {
        val exception = shouldThrow<IllegalArgumentException> {
            AiModelContextChunk(
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
            AiModelContextChunk(
                content = "Use two parts water for one part rice.",
                resourcePath = "subject-documents/culinary_expert/rice.txt",
                chunkIndex = -1,
            )
        }

        exception.message shouldContain "chunkIndex"
    }
}
