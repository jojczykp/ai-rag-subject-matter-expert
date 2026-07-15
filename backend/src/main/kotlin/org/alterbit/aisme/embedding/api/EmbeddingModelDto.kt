package org.alterbit.aisme.embedding.api

import org.alterbit.aisme.embedding.catalog.EmbeddingModelAvailability
import org.alterbit.aisme.embedding.catalog.EmbeddingModelDescriptor
import org.alterbit.aisme.embedding.catalog.EmbeddingModelMode
import org.alterbit.aisme.embedding.catalog.EmbeddingModelRuntime

data class EmbeddingModelDto(
    val id: String,
    val enabled: Boolean,
    val displayName: String,
    val runtime: EmbeddingModelRuntime,
    val mode: EmbeddingModelMode,
    val availability: EmbeddingModelAvailability,
    val version: String?,
    val dimensions: Int?,
    val availableOffline: Boolean,
)

fun EmbeddingModelDescriptor.toDto(): EmbeddingModelDto =
    EmbeddingModelDto(
        id = id,
        enabled = enabled,
        displayName = displayName,
        runtime = runtime,
        mode = mode,
        availability = availability,
        version = version,
        dimensions = dimensions,
        availableOffline = runtime == EmbeddingModelRuntime.ONNX,
    )
