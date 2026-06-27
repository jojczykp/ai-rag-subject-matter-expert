package org.alterbit.aisme.embedding

data class EmbeddingVector(
    val values: List<Double>,
    val modelId: String,
    val modelVersion: String,
    val dimensions: Int,
)
