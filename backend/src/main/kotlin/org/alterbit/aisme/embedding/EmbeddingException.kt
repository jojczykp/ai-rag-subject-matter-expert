package org.alterbit.aisme.embedding

class EmbeddingException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
