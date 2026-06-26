package org.alterbit.aisme.persistence

import java.util.UUID
import org.springframework.data.repository.CrudRepository

interface SourceDocumentRepository : CrudRepository<SourceDocumentRecord, UUID> {
    fun findByResourcePath(resourcePath: String): SourceDocumentRecord?
}
