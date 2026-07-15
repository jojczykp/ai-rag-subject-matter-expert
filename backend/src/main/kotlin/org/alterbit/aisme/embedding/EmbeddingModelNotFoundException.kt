package org.alterbit.aisme.embedding

class EmbeddingModelNotFoundException(
    val modelId: String,
) : RuntimeException("Embedding model '$modelId' was not found or is not enabled")
