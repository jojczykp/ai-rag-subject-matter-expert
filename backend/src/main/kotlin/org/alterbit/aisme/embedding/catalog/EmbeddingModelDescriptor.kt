package org.alterbit.aisme.embedding.catalog

import org.alterbit.aisme.assets.ModelAsset
import org.alterbit.aisme.assets.ModelAssetOwner

data class EmbeddingModelDescriptor(
    val id: String,
    override val enabled: Boolean,
    override val downloadMissingAssetsOnStartup: Boolean = true,
    override val assets: List<ModelAsset> = emptyList(),
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
) : ModelAssetOwner
