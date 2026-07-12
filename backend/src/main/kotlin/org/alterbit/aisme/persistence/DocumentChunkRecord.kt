package org.alterbit.aisme.persistence

import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("document_chunk")
data class DocumentChunkRecord(
    @Id
    val id: UUID? = null,
    val sourceDocumentId: UUID,
    val chunkIndex: Int,
    val content: String,
    val startOffset: Int,
    val endOffset: Int,
    val chunkingStrategyVersion: String,
)
