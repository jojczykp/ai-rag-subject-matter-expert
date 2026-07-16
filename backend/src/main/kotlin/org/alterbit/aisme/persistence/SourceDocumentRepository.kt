package org.alterbit.aisme.persistence

import java.util.UUID
import org.springframework.data.repository.CrudRepository

interface SourceDocumentRepository : CrudRepository<SourceDocumentRecord, UUID> {
    fun findBySubjectIdAndResourcePath(subjectId: String, resourcePath: String): SourceDocumentRecord?

    fun findBySubjectIdOrderByResourcePath(subjectId: String): List<SourceDocumentRecord>

    fun findAllByOrderBySubjectIdAscResourcePathAsc(): List<SourceDocumentRecord>
}
