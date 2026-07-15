package org.alterbit.aisme.embedding

data class EmbeddingModelsResponseDto(
    val defaultEmbeddingModelId: String?,
    val embeddingModels: List<EmbeddingModelDto>,
)
