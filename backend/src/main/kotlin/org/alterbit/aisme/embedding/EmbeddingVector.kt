package org.alterbit.aisme.embedding

import org.alterbit.aisme.embedding.catalog.EmbeddingModelMetadata

data class EmbeddingVector(
    val values: List<Double>,
    val model: EmbeddingModelMetadata,
)
