package org.alterbit.aisme.assets

import java.io.InputStream
import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ModelAssetDownloader(
    private val platform: ModelAssetPlatform = ModelAssetPlatform.current(),
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
            URI(url).toURL().openStream().use { input ->
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING)
            }
            asset.archive
                ?.let { archive -> installArchiveAsset(temporaryFile, asset, archive) }
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

    private fun installArchiveAsset(
        archiveFile: Path,
        asset: ModelAsset,
        archive: ModelAssetArchive,
    ) {
        val targetDirectory = requireNotNull(asset.path.parent) {
            "Model asset path must include a parent directory: ${asset.path}"
        }
        Files.createDirectories(targetDirectory)
        archiveFile.openArchive(archive.format).use { input ->
            when (archive.format) {
                ModelAssetArchiveFormat.TAR_GZ -> installFromTarGz(input as TarArchiveInputStream, asset, archive)
                ModelAssetArchiveFormat.ZIP -> installFromZip(input as ZipInputStream, asset, archive)
            }
        }
        Files.deleteIfExists(archiveFile)
        require(Files.isRegularFile(asset.path)) {
            "Archive for ${asset.label} asset used by '${asset.modelId}' did not contain executable '${archive.executableName}'"
        }
        if (archive.format != ModelAssetArchiveFormat.ZIP) {
            asset.path.toFile().setExecutable(true)
            replaceEmptyLibraryAliasSymlinks(targetDirectory)
        }
    }

    private fun Path.openArchive(format: ModelAssetArchiveFormat): InputStream =
        when (format) {
            ModelAssetArchiveFormat.TAR_GZ -> TarArchiveInputStream(GZIPInputStream(Files.newInputStream(this)))
            ModelAssetArchiveFormat.ZIP -> ZipInputStream(Files.newInputStream(this))
        }

    private fun installFromTarGz(
        input: TarArchiveInputStream,
        asset: ModelAsset,
        archive: ModelAssetArchive,
    ) {
        generateSequence { input.nextEntry }
            .filterNot { entry -> entry.isDirectory }
            .filter { entry -> entry.name.isInstallableArchiveEntry(archive.executableName) }
            .forEach { entry ->
                val target = archiveTargetPath(asset, archive, entry.name)
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
            }
    }

    private fun installFromZip(
        input: ZipInputStream,
        asset: ModelAsset,
        archive: ModelAssetArchive,
    ) {
        generateSequence { input.nextEntry }
            .filterNot { entry -> entry.isDirectory }
            .filter { entry -> entry.name.isInstallableArchiveEntry(archive.executableName) }
            .forEach { entry ->
                val target = archiveTargetPath(asset, archive, entry.name)
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
            }
    }

    private fun archiveTargetPath(
        asset: ModelAsset,
        archive: ModelAssetArchive,
        entryName: String,
    ): Path {
        val targetDirectory = requireNotNull(asset.path.parent)
        val fileName = Path.of(entryName).fileName.toString()
        return if (fileName == archive.executableName) {
            asset.path
        } else {
            targetDirectory.resolve(fileName)
        }
    }

    private fun String.isInstallableArchiveEntry(executableName: String): Boolean {
        val fileName = Path.of(this).fileName.toString()
        return fileName == executableName ||
            fileName.endsWith(".dylib") ||
            fileName.endsWith(".so") ||
            fileName.contains(".so.") ||
            fileName.endsWith(".dll")
    }

    private fun replaceEmptyLibraryAliasSymlinks(directory: Path) {
        val files = Files.list(directory).use { stream ->
            stream.filter(Files::isRegularFile).toList()
        }
        files
            .filter { file -> Files.size(file) == 0L }
            .forEach { alias ->
                val target = libraryAliasTarget(alias, files)
                if (target != null) {
                    Files.delete(alias)
                    Files.createSymbolicLink(alias, target.fileName)
                }
            }
    }

    private fun libraryAliasTarget(alias: Path, files: List<Path>): Path? {
        val prefixes = libraryAliasTargetPrefixes(alias.fileName.toString())
        return files
            .filter { candidate -> Files.size(candidate) > 0L }
            .filter { candidate -> prefixes.any { prefix -> candidate.fileName.toString().startsWith(prefix) } }
            .maxByOrNull { candidate -> candidate.fileName.toString().length }
    }

    private fun libraryAliasTargetPrefixes(fileName: String): List<String> =
        when {
            fileName.endsWith(".dylib") -> listOf(
                "${fileName.removeSuffix(".dylib")}.",
                "${fileName.removeSuffix(".0.dylib")}.",
            )

            fileName.endsWith(".so") -> listOf("${fileName.removeSuffix(".so")}.so.")
            fileName.contains(".so.") -> listOf("$fileName.")
            else -> emptyList()
        }
}
