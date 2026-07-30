package org.alterbit.aisme.assets

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ModelAssetDownloader(
    private val platform: ModelAssetPlatform = ModelAssetPlatform.current(),
    private val fileDownloader: ModelAssetFileDownloader = ModelAssetFileDownloader(),
    private val archiveInstaller: ModelAssetArchiveInstaller = ModelAssetArchiveInstaller(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun downloadMissing(assets: List<ModelAsset>) {
        val matchingAssets = assets.filter { asset -> asset.matches(platform) }
        val missingAssets = matchingAssets.filterNot { asset -> Files.isRegularFile(asset.path) }.distinctBy(ModelAsset::path)
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

        val targetDirectory = requireNotNull(asset.path.parent) {
            "Model asset path must include a parent directory: ${asset.path}"
        }
        Files.createDirectories(targetDirectory)
        val temporaryFile = Files.createTempFile(targetDirectory, "${asset.path.fileName}.", ".download")
        try {
            fileDownloader.download(url, temporaryFile, asset)
            asset.archive
                ?.let { archive -> archiveInstaller.install(temporaryFile, asset, archive) }
                ?: moveDownloadedAsset(temporaryFile, asset.path)
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
