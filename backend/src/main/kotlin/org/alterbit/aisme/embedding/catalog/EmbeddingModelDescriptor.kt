package org.alterbit.aisme.embedding.catalog

data class EmbeddingModelDescriptor(
    val id: String,
    val enabled: Boolean,
    val displayOrder: Int?,
    val displayName: String,
    val runtime: EmbeddingModelRuntime,
    val mode: EmbeddingModelMode,
    val availability: EmbeddingModelAvailability,
    val version: String?,
    val dimensions: Int?,
    val baseUrl: String?,
    val modelName: String?,
    val modelPath: String?,
    val tokenizerPath: String?,
)
