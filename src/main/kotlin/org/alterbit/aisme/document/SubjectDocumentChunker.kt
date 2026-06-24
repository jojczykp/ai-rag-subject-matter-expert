package org.alterbit.aisme.document

import org.springframework.stereotype.Component

@Component
class SubjectDocumentChunker(
    private val properties: SubjectDocumentsProperties,
) {
    fun chunk(document: SubjectDocumentContent): List<SubjectDocumentChunk> {
        if (document.content.isBlank()) {
            throw SubjectDocumentsException("Subject document is empty: ${document.relativePath}")
        }

        val chunks = mutableListOf<SubjectDocumentChunk>()
        val step = properties.chunkSize - properties.chunkOverlap
        var startOffset = 0
        var index = 0

        while (startOffset < document.content.length) {
            val endOffset = minOf(startOffset + properties.chunkSize, document.content.length)
            chunks += SubjectDocumentChunk(
                documentPath = document.relativePath,
                index = index,
                content = document.content.substring(startOffset, endOffset),
                startOffset = startOffset,
                endOffset = endOffset,
            )

            if (endOffset == document.content.length) {
                break
            }

            startOffset += step
            index += 1
        }

        return chunks
    }
}
