package org.alterbit.aisme.embedding

data class EmbeddingModelDescriptor(
    val id: String,
    val enabled: Boolean,
    val displayOrder: Int?,
    val displayName: String,
    val runtime: EmbeddingModelRuntime,
    val mode: EmbeddingModelMode,
    val version: String?,
    val dimensions: Int?,
)
