package org.alterbit.aisme.assets

import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ModelAssetDownloader {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun downloadMissing(assets: List<ModelAsset>) {
        val missingAssets = assets.filterNot { asset -> Files.isRegularFile(asset.path) }
        if (missingAssets.isEmpty()) {
            logger.info("All configured local model assets are already present")
            return
        }

        logger.info("Downloading {} missing local model asset(s)", missingAssets.size)
        missingAssets.forEach(::download)
    }

    private fun download(asset: ModelAsset) {
        val url = requireNotNull(asset.url) {
            "Missing download URL for ${asset.label} asset '${asset.path}' used by '${asset.modelId}'"
        }
        logger.info(
            "Downloading {} asset for '{}' from '{}' to '{}'",
            asset.label,
            asset.modelId,
            url,
            asset.path,
        )

        asset.path.parent?.let(Files::createDirectories)
        val temporaryFile = Files.createTempFile(asset.path.parent, "${asset.path.fileName}.", ".download")
        try {
            URI(url).toURL().openStream().use { input ->
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING)
            }
            moveDownloadedAsset(temporaryFile, asset.path)
            logger.info("Downloaded {} asset for '{}' to '{}'", asset.label, asset.modelId, asset.path)
        } catch (ex: Exception) {
            Files.deleteIfExists(temporaryFile)
            logger.error("Failed downloading {} asset for '{}' to '{}'", asset.label, asset.modelId, asset.path, ex)
            throw ex
        }
    }

    private fun moveDownloadedAsset(
        temporaryFile: Path,
        targetFile: Path,
    ) {
        try {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
