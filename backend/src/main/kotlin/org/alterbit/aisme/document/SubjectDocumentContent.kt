package org.alterbit.aisme.document

data class SubjectDocumentContent(
    val subjectId: String,
    val relativePath: String,
    val content: String,
) {
    init {
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
    }
}
