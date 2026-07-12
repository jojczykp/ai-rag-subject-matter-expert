package org.alterbit.aisme.embedding

data class EmbeddingVector(
    val values: List<Double>,
    val model: EmbeddingModelMetadata,
)
