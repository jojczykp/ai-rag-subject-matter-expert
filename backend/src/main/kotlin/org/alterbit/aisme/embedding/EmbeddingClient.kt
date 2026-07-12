package org.alterbit.aisme.embedding

interface EmbeddingClient {
    fun embed(text: String): EmbeddingVector
}
