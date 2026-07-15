package org.alterbit.aisme.embedding.catalog

data class EmbeddingModelsResponseDto(
    val defaultEmbeddingModelId: String?,
    val embeddingApiTimeoutSeconds: Long,
    val embeddingModels: List<EmbeddingModelDto>,
)
