package org.alterbit.aisme.retrieval

import org.alterbit.aisme.embedding.EmbeddingModelMetadata

data class RelevantChunkRequest(
    val embedding: List<Double>,
    val embeddingModel: EmbeddingModelMetadata,
    val chunkingStrategyVersion: String,
    val limit: Int,
)
