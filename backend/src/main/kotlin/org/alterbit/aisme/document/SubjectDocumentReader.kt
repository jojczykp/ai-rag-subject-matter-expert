package org.alterbit.aisme.document

import java.io.IOException
import java.nio.charset.StandardCharsets
import org.springframework.stereotype.Component

@Component
class SubjectDocumentReader {
    fun read(document: SubjectDocumentResource): SubjectDocumentContent {
        val resource = document.resource
        val content = try {
            resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (ex: IOException) {
            throw SubjectDocumentsException("Subject document could not be read: ${document.relativePath}", ex)
        }

        return SubjectDocumentContent(
            subjectId = document.subjectId,
            relativePath = document.relativePath,
            content = content,
        )
    }
}
