package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SubjectDocumentChunkerTest {
    @Test
    fun `creates one chunk when content is shorter than chunk size`() {
        val documentsProperties = documentsProperties(chunkSize = 100, chunkOverlap = 10)

        val chunks = chunker().chunk(
            document = SubjectDocumentContent(
                subjectId = "culinary-expert",
                relativePath = "reference.txt",
                content = "Short content",
            ),
            documentsProperties = documentsProperties,
        )

        chunks shouldBe listOf(
            SubjectDocumentChunk(
                subjectId = "culinary-expert",
                documentPath = "reference.txt",
                chunkingStrategyVersion = "character-count-v1:size=100:overlap=10",
                index = 0,
                content = "Short content",
                startOffset = 0,
                endOffset = 13,
            ),
        )
    }

    @Test
    fun `creates deterministic overlapping chunks`() {
        val documentsProperties = documentsProperties(chunkSize = 5, chunkOverlap = 2)

        val chunks = chunker().chunk(
            document = SubjectDocumentContent(
                subjectId = "culinary-expert",
                relativePath = "nested/reference.txt",
                content = "abcdefghijkl",
            ),
            documentsProperties = documentsProperties,
        )

        chunks shouldBe listOf(
            SubjectDocumentChunk(
                subjectId = "culinary-expert",
                documentPath = "nested/reference.txt",
                chunkingStrategyVersion = "character-count-v1:size=5:overlap=2",
                index = 0,
                content = "abcde",
                startOffset = 0,
                endOffset = 5,
            ),
            SubjectDocumentChunk(
                subjectId = "culinary-expert",
                documentPath = "nested/reference.txt",
                chunkingStrategyVersion = "character-count-v1:size=5:overlap=2",
                index = 1,
                content = "defgh",
                startOffset = 3,
                endOffset = 8,
            ),
            SubjectDocumentChunk(
                subjectId = "culinary-expert",
                documentPath = "nested/reference.txt",
                chunkingStrategyVersion = "character-count-v1:size=5:overlap=2",
                index = 2,
                content = "ghijk",
                startOffset = 6,
                endOffset = 11,
            ),
            SubjectDocumentChunk(
                subjectId = "culinary-expert",
                documentPath = "nested/reference.txt",
                chunkingStrategyVersion = "character-count-v1:size=5:overlap=2",
                index = 3,
                content = "jkl",
                startOffset = 9,
                endOffset = 12,
            ),
        )
    }

    private fun chunker(): SubjectDocumentChunker =
        SubjectDocumentChunker()

    private fun documentsProperties(
        chunkSize: Int = 700,
        chunkOverlap: Int = 100,
    ): SubjectDocumentsProperties =
        SubjectDocumentsProperties(
            chunkSize = chunkSize,
            chunkOverlap = chunkOverlap,
        )
}
