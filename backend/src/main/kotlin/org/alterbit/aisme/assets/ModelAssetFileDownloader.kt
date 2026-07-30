package org.alterbit.aisme.assets

import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale
import org.slf4j.LoggerFactory

private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
private const val DOWNLOAD_PROGRESS_PERCENT_STEP = 10
private const val UNKNOWN_SIZE_PROGRESS_BYTES = 64L * 1024 * 1024
private const val BYTES_IN_KIB = 1024L
private const val BYTES_IN_MIB = BYTES_IN_KIB * 1024L
private const val BYTES_IN_GIB = BYTES_IN_MIB * 1024L

class ModelAssetFileDownloader {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun download(
        url: String,
        targetFile: Path,
        asset: ModelAsset,
    ) {
        val connection = URI(url).toURL().openConnection()
        val totalBytes = connection.contentLengthLong.takeIf { bytes -> bytes > 0L }
        connection.getInputStream().use { input ->
            Files.newOutputStream(targetFile, StandardOpenOption.TRUNCATE_EXISTING).use { output ->
                copyWithProgress(input, output, asset, totalBytes)
            }
        }
    }

    private fun copyWithProgress(
        input: InputStream,
        output: OutputStream,
        asset: ModelAsset,
        totalBytes: Long?,
    ) {
        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
        var downloadedBytes = 0L
        var nextProgressPercent = DOWNLOAD_PROGRESS_PERCENT_STEP
        var nextUnknownSizeProgressBytes = UNKNOWN_SIZE_PROGRESS_BYTES

        generateSequence { input.read(buffer).takeIf { bytesRead -> bytesRead >= 0 } }
            .forEach { bytesRead ->
                output.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                logProgressIfNeeded(
                    asset = asset,
                    totalBytes = totalBytes,
                    downloadedBytes = downloadedBytes,
                    nextProgressPercent = nextProgressPercent,
                    nextUnknownSizeProgressBytes = nextUnknownSizeProgressBytes,
                ).also { progress ->
                    nextProgressPercent = progress.nextProgressPercent
                    nextUnknownSizeProgressBytes = progress.nextUnknownSizeProgressBytes
                }
            }
    }

    private fun logProgressIfNeeded(
        asset: ModelAsset,
        totalBytes: Long?,
        downloadedBytes: Long,
        nextProgressPercent: Int,
        nextUnknownSizeProgressBytes: Long,
    ): DownloadProgress =
        if (totalBytes != null) {
            logKnownSizeProgressIfNeeded(asset, totalBytes, downloadedBytes, nextProgressPercent)
        } else {
            logUnknownSizeProgressIfNeeded(asset, downloadedBytes, nextUnknownSizeProgressBytes)
        }

    private fun logKnownSizeProgressIfNeeded(
        asset: ModelAsset,
        totalBytes: Long,
        downloadedBytes: Long,
        nextProgressPercent: Int,
    ): DownloadProgress {
        val progressPercent = (downloadedBytes * 100 / totalBytes).toInt()
        if (progressPercent < nextProgressPercent || progressPercent == 100) {
            return DownloadProgress(
                nextProgressPercent = nextProgressPercent,
                nextUnknownSizeProgressBytes = UNKNOWN_SIZE_PROGRESS_BYTES,
            )
        }

        logger.info(
            "Downloaded {}% of {} asset for '{}' ({}/{})",
            nextProgressPercent,
            asset.label,
            asset.modelId,
            downloadedBytes.formatBytes(),
            totalBytes.formatBytes(),
        )
        var followingProgressPercent = nextProgressPercent
        while (followingProgressPercent <= progressPercent) {
            followingProgressPercent += DOWNLOAD_PROGRESS_PERCENT_STEP
        }
        return DownloadProgress(
            nextProgressPercent = followingProgressPercent,
            nextUnknownSizeProgressBytes = UNKNOWN_SIZE_PROGRESS_BYTES,
        )
    }

    private fun logUnknownSizeProgressIfNeeded(
        asset: ModelAsset,
        downloadedBytes: Long,
        nextUnknownSizeProgressBytes: Long,
    ): DownloadProgress {
        if (downloadedBytes < nextUnknownSizeProgressBytes) {
            return DownloadProgress(
                nextProgressPercent = DOWNLOAD_PROGRESS_PERCENT_STEP,
                nextUnknownSizeProgressBytes = nextUnknownSizeProgressBytes,
            )
        }

        logger.info(
            "Downloaded {} of {} asset for '{}'",
            downloadedBytes.formatBytes(),
            asset.label,
            asset.modelId,
        )
        var followingProgressBytes = nextUnknownSizeProgressBytes
        while (followingProgressBytes <= downloadedBytes) {
            followingProgressBytes += UNKNOWN_SIZE_PROGRESS_BYTES
        }
        return DownloadProgress(
            nextProgressPercent = DOWNLOAD_PROGRESS_PERCENT_STEP,
            nextUnknownSizeProgressBytes = followingProgressBytes,
        )
    }

    private fun Long.formatBytes(): String =
        when {
            this >= BYTES_IN_GIB -> "%.1f GiB".format(Locale.US, this.toDouble() / BYTES_IN_GIB)
            this >= BYTES_IN_MIB -> "%.1f MiB".format(Locale.US, this.toDouble() / BYTES_IN_MIB)
            this >= BYTES_IN_KIB -> "%.1f KiB".format(Locale.US, this.toDouble() / BYTES_IN_KIB)
            else -> "$this B"
        }
}

private data class DownloadProgress(
    val nextProgressPercent: Int,
    val nextUnknownSizeProgressBytes: Long,
)
