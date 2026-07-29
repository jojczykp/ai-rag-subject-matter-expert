package org.alterbit.aisme.assets

interface ModelAssetOwner {
    val enabled: Boolean
    val downloadMissingAssetsOnStartup: Boolean
    val assets: List<ModelAsset>
}

fun ModelAssetOwner.downloadableAssets(): List<ModelAsset> =
    if (enabled && downloadMissingAssetsOnStartup) {
        assets
    } else {
        emptyList()
    }
