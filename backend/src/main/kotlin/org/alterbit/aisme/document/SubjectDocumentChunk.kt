package org.alterbit.aisme.document

data class SubjectDocumentChunk(
    val documentPath: String,
    val index: Int,
    val content: String,
    val startOffset: Int,
    val endOffset: Int,
)
