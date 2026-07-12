package org.alterbit.aisme.document

import org.springframework.stereotype.Component

@Component
class SubjectDocumentValidator {
    fun validate(document: SubjectDocumentResource) {
        val resource = document.resource
        if (!resource.exists()) {
            throw SubjectDocumentsException("Subject document does not exist: ${document.relativePath}")
        }
        if (!resource.isReadable) {
            throw SubjectDocumentsException("Subject document is not readable: ${document.relativePath}")
        }
    }

    fun validate(document: SubjectDocumentContent) {
        if (document.content.isBlank()) {
            throw SubjectDocumentsException("Subject document is empty: ${document.relativePath}")
        }
    }
}
