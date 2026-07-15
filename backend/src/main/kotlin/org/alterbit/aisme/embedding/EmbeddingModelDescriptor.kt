package org.alterbit.aisme.embedding

data class EmbeddingModelDescriptor(
    val id: String,
    val enabled: Boolean,
    val displayName: String,
    val runtime: EmbeddingModelRuntime,
    val version: String?,
    val dimensions: Int?,
)
