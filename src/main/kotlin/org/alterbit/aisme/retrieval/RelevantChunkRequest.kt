package org.alterbit.aisme.retrieval

data class RelevantChunkRequest(
    val embedding: List<Double>,
    val embeddingModelId: String,
    val embeddingModelVersion: String,
    val embeddingDimensions: Int,
    val chunkingStrategyVersion: String,
    val limit: Int,
)
