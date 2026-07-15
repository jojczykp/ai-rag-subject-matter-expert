package org.alterbit.aisme.embedding.catalog

data class EmbeddingModelsResponseDto(
    val defaultEmbeddingModelId: String?,
    val embeddingModels: List<EmbeddingModelDto>,
)
