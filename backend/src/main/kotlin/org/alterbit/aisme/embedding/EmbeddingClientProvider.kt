package org.alterbit.aisme.embedding

fun interface EmbeddingClientProvider {
    fun clients(): List<EmbeddingClient>
}
