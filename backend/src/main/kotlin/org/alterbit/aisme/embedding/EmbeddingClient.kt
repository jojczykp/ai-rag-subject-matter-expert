package org.alterbit.aisme.embedding

interface EmbeddingClient {
    val modelId: String
    val model: EmbeddingModelMetadata

    fun embed(text: String): EmbeddingVector
}
