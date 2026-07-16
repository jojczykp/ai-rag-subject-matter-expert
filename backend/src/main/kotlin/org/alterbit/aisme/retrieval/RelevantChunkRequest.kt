package org.alterbit.aisme.retrieval

import org.alterbit.aisme.embedding.catalog.EmbeddingModelMetadata

data class RelevantChunkRequest(
    val subjectId: String,
    val embedding: List<Double>,
    val embeddingModel: EmbeddingModelMetadata,
    val chunkingStrategyVersion: String,
    val limit: Int,
) {
    init {
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
    }
}
