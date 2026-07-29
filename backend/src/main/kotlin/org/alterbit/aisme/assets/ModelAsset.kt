package org.alterbit.aisme.assets

import java.nio.file.Path

data class ModelAsset(
    val modelId: String,
    val label: String,
    val path: Path,
    val url: String?,
) {
    init {
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(label.isNotBlank()) { "label must not be blank" }
        require(url == null || url.isNotBlank()) { "url must not be blank when configured" }
    }
}
