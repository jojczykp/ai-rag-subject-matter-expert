package org.alterbit.aisme.embedding.catalog

class EmbeddingModelNotFoundException(
    val modelId: String,
) : RuntimeException("Embedding model '$modelId' was not found or is not enabled")
