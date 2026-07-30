package org.alterbit.aisme.assets

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream

class ModelAssetArchiveInstaller(
    private val librarySymlinkRepairer: ModelAssetLibrarySymlinkRepairer = ModelAssetLibrarySymlinkRepairer(),
) {
    fun install(
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
            librarySymlinkRepairer.repair(targetDirectory)
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
}
