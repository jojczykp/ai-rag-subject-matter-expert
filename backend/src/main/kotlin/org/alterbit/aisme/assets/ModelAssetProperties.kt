package org.alterbit.aisme.assets

import java.nio.file.Path

data class ModelAssetProperties(
    val label: String,
    val path: String,
    val url: String? = null,
) {
    init {
        require(label.isNotBlank()) { "model asset label must not be blank" }
        require(path.isNotBlank()) { "model asset path must not be blank" }
        require(url == null || url.isNotBlank()) { "model asset url must not be blank when configured" }
    }

    fun toModelAsset(modelId: String): ModelAsset =
        ModelAsset(
            modelId = modelId,
            label = label,
            path = Path.of(path),
            url = url,
        )
}
