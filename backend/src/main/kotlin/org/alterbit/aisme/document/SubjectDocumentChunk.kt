package org.alterbit.aisme.document

data class SubjectDocumentChunk(
    val subjectId: String,
    val documentPath: String,
    val index: Int,
    val chunkingStrategyVersion: String,
    val content: String,
    val startOffset: Int,
    val endOffset: Int,
) {
    init {
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
        require(chunkingStrategyVersion.isNotBlank()) { "chunkingStrategyVersion must not be blank" }
    }
}
