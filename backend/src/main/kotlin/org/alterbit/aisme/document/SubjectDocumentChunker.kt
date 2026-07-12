package org.alterbit.aisme.document

import org.springframework.stereotype.Component

@Component
class SubjectDocumentChunker(
    private val properties: SubjectDocumentsProperties,
) {
    fun chunk(document: SubjectDocumentContent): List<SubjectDocumentChunk> {
        val step = properties.chunkSize - properties.chunkOverlap

        return generateSequence(0) { startOffset -> startOffset + step }
            .takeWhile { startOffset -> startOffset < document.content.length }
            .mapIndexed { index, startOffset ->
                val endOffset = minOf(startOffset + properties.chunkSize, document.content.length)
                SubjectDocumentChunk(
                    documentPath = document.relativePath,
                    index = index,
                    content = document.content.substring(startOffset, endOffset),
                    startOffset = startOffset,
                    endOffset = endOffset,
                )
            }
            .toList()
    }
}
