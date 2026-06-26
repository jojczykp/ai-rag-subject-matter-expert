package org.alterbit.aisme.persistence

import java.util.UUID
import org.springframework.data.repository.CrudRepository

interface DocumentChunkRepository : CrudRepository<DocumentChunkRecord, UUID> {
    fun findBySourceDocumentIdOrderByChunkIndex(sourceDocumentId: UUID): List<DocumentChunkRecord>
}
