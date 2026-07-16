package org.alterbit.aisme.document

import org.springframework.core.io.Resource

data class SubjectDocumentResource(
    val subjectId: String,
    val relativePath: String,
    val resource: Resource,
) {
    init {
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
    }
}
