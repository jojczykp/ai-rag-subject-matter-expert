package org.alterbit.aisme.document

import java.io.IOException
import java.nio.charset.StandardCharsets
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

        val content = try {
            resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (ex: IOException) {
            throw SubjectDocumentsException("Subject document could not be read: ${document.relativePath}", ex)
        }

        if (content.isBlank()) {
            throw SubjectDocumentsException("Subject document is empty: ${document.relativePath}")
        }
    }
}
