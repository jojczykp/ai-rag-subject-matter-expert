package org.alterbit.aisme.document

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class SubjectDocumentChunkerTest {
    @Test
    fun `creates one chunk when content is shorter than chunk size`() {
        val chunks = chunker(chunkSize = 100, chunkOverlap = 10).chunk(
            SubjectDocumentContent(
                relativePath = "reference.txt",
                content = "Short content",
            ),
        )

        chunks shouldBe listOf(
            SubjectDocumentChunk(
                documentPath = "reference.txt",
                index = 0,
                content = "Short content",
                startOffset = 0,
                endOffset = 13,
            ),
        )
    }

    @Test
    fun `creates deterministic overlapping chunks`() {
        val chunks = chunker(chunkSize = 5, chunkOverlap = 2).chunk(
            SubjectDocumentContent(
                relativePath = "nested/reference.txt",
                content = "abcdefghijkl",
            ),
        )

        chunks shouldBe listOf(
            SubjectDocumentChunk(
                documentPath = "nested/reference.txt",
                index = 0,
                content = "abcde",
                startOffset = 0,
                endOffset = 5,
            ),
            SubjectDocumentChunk(
                documentPath = "nested/reference.txt",
                index = 1,
                content = "defgh",
                startOffset = 3,
                endOffset = 8,
            ),
            SubjectDocumentChunk(
                documentPath = "nested/reference.txt",
                index = 2,
                content = "ghijk",
                startOffset = 6,
                endOffset = 11,
            ),
            SubjectDocumentChunk(
                documentPath = "nested/reference.txt",
                index = 3,
                content = "jkl",
                startOffset = 9,
                endOffset = 12,
            ),
        )
    }

    @Test
    fun `rejects blank content`() {
        val exception = shouldThrow<SubjectDocumentsException> {
            chunker().chunk(
                SubjectDocumentContent(
                    relativePath = "blank.txt",
                    content = "   ",
                ),
            )
        }

        exception.message shouldContain "empty"
    }

    private fun chunker(
        chunkSize: Int = 1000,
        chunkOverlap: Int = 150,
    ): SubjectDocumentChunker =
        SubjectDocumentChunker(
            SubjectDocumentsProperties(
                chunkSize = chunkSize,
                chunkOverlap = chunkOverlap,
            ),
        )
}
