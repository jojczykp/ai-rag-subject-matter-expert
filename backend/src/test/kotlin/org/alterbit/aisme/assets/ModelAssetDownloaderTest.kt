package org.alterbit.aisme.assets

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModelAssetDownloaderTest {
    private val downloader = ModelAssetDownloader()

    @Test
    fun `downloads missing asset`(@TempDir tempDirectory: Path) {
        val sourceFile = tempDirectory.resolve("source.bin")
        val targetFile = tempDirectory.resolve("models/model.bin")
        Files.writeString(sourceFile, "downloaded content")

        downloader.downloadMissing(
            listOf(
                ModelAsset(
                    modelId = "local-model",
                    label = "model",
                    path = targetFile,
                    url = sourceFile.toUri().toString(),
                ),
            ),
        )

        Files.exists(targetFile) shouldBe true
        Files.readString(targetFile) shouldBe "downloaded content"
    }

    @Test
    fun `keeps existing asset unchanged`(@TempDir tempDirectory: Path) {
        val sourceFile = tempDirectory.resolve("source.bin")
        val targetFile = tempDirectory.resolve("models/model.bin")
        Files.writeString(sourceFile, "downloaded content")
        Files.createDirectories(targetFile.parent)
        Files.writeString(targetFile, "existing content")

        downloader.downloadMissing(
            listOf(
                ModelAsset(
                    modelId = "local-model",
                    label = "model",
                    path = targetFile,
                    url = sourceFile.toUri().toString(),
                ),
            ),
        )

        Files.readString(targetFile) shouldBe "existing content"
    }

    @Test
    fun `fails when missing asset has no download url`(@TempDir tempDirectory: Path) {
        val exception = shouldThrow<IllegalArgumentException> {
            downloader.downloadMissing(
                listOf(
                    ModelAsset(
                        modelId = "local-model",
                        label = "model",
                        path = tempDirectory.resolve("models/model.bin"),
                        url = null,
                    ),
                ),
            )
        }

        exception.message shouldBe
            "Missing download URL for model asset '${tempDirectory.resolve("models/model.bin")}' used by 'local-model'"
    }
}
