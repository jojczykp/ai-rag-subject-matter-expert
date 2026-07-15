package org.alterbit.aisme.embedding.api

data class EmbeddingModelsResponseDto(
    val defaultEmbeddingModelId: String?,
    val embeddingApiTimeoutSeconds: Long,
    val embeddingModels: List<EmbeddingModelDto>,
)
