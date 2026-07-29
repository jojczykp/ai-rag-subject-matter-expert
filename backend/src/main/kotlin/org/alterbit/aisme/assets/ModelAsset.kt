package org.alterbit.aisme.assets

import java.nio.file.Path

data class ModelAsset(
    val modelId: String,
    val label: String,
    val path: Path,
    val url: String?,
    val os: ModelAssetOperatingSystem? = null,
    val arch: ModelAssetArchitecture? = null,
    val archive: ModelAssetArchive? = null,
) {
    init {
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(label.isNotBlank()) { "label must not be blank" }
        require(url == null || url.isNotBlank()) { "url must not be blank when configured" }
    }
}

data class ModelAssetArchive(
    val format: ModelAssetArchiveFormat,
    val executableName: String,
) {
    init {
        require(executableName.isNotBlank()) { "executableName must not be blank" }
    }
}

enum class ModelAssetArchiveFormat {
    TAR_GZ,
    ZIP,
}
