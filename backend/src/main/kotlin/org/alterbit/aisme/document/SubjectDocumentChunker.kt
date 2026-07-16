package org.alterbit.aisme.document

import org.springframework.stereotype.Component

@Component
class SubjectDocumentChunker {
    fun chunk(
        document: SubjectDocumentContent,
        documentsProperties: SubjectDocumentsProperties,
    ): List<SubjectDocumentChunk> {
        val step = documentsProperties.chunkSize - documentsProperties.chunkOverlap

        return generateSequence(0) { startOffset -> startOffset + step }
            .takeWhile { startOffset -> startOffset < document.content.length }
            .mapIndexed { index, startOffset ->
                val endOffset = minOf(startOffset + documentsProperties.chunkSize, document.content.length)
                SubjectDocumentChunk(
                    subjectId = document.subjectId,
                    documentPath = document.relativePath,
                    index = index,
                    chunkingStrategyVersion = documentsProperties.chunkingStrategyVersion(),
                    content = document.content.substring(startOffset, endOffset),
                    startOffset = startOffset,
                    endOffset = endOffset,
                )
            }
            .toList()
    }
}
