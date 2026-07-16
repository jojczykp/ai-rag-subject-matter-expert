package org.alterbit.aisme.retrieval

import java.util.UUID

data class RelevantChunk(
    val chunkId: UUID,
    val sourceDocumentId: UUID,
    val subjectId: String,
    val resourcePath: String,
    val chunkIndex: Int,
    val content: String,
    val startOffset: Int,
    val endOffset: Int,
    val cosineDistance: Double,
)
