package org.alterbit.aisme.persistence

import java.time.Instant
import java.util.UUID
import org.alterbit.aisme.embedding.EmbeddingVector

data class SaveChunkEmbeddingRequest(
    val documentChunkId: UUID,
    val embedding: EmbeddingVector,
    val chunkingStrategyVersion: String,
    val embeddedAt: Instant,
)
