package org.alterbit.aisme.document

import org.springframework.core.io.Resource

data class SubjectDocumentResource(
    val relativePath: String,
    val resource: Resource,
)
