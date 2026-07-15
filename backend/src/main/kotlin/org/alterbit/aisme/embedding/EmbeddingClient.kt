package org.alterbit.aisme.embedding

import org.alterbit.aisme.embedding.catalog.EmbeddingModelMetadata

interface EmbeddingClient {
    val modelId: String
    val model: EmbeddingModelMetadata

    fun embed(text: String): EmbeddingVector
}
