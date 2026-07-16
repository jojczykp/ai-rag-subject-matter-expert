package org.alterbit.aisme.persistence

import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("source_document")
data class SourceDocumentRecord(
    @Id
    val id: UUID? = null,
    val subjectId: String,
    val resourcePath: String,
    val contentHash: String,
    val indexedAt: Instant,
) {
    init {
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
    }
}
