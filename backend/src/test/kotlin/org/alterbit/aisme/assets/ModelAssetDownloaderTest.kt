package org.alterbit.aisme.assets

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModelAssetDownloaderTest {
    private val downloader = ModelAssetDownloader(
        platform = ModelAssetPlatform(
            os = ModelAssetOperatingSystem.LINUX,
            arch = ModelAssetArchitecture.X86_64,
        ),
    )

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
    fun `skips asset for another platform`(@TempDir tempDirectory: Path) {
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
                    os = ModelAssetOperatingSystem.MACOS,
                    arch = ModelAssetArchitecture.AARCH64,
                ),
            ),
        )

        Files.exists(targetFile) shouldBe false
    }

    @Test
    fun `installs executable and libraries from zip archive`(@TempDir tempDirectory: Path) {
        val archiveFile = tempDirectory.resolve("llama.zip")
        val executable = tempDirectory.resolve("models/llama/bin/llama-server")
        createZipArchive(
            archiveFile = archiveFile,
            entries = mapOf(
                "llama-b9892/bin/llama-server.exe" to "server",
                "llama-b9892/bin/libllama.dll" to "dll",
                "llama-b9892/bin/ignored.txt" to "ignored",
            ),
        )

        downloader.downloadMissing(
            listOf(
                ModelAsset(
                    modelId = "chat-runtime:embedded-llama",
                    label = "llama-server Windows x64",
                    path = executable,
                    url = archiveFile.toUri().toString(),
                    os = ModelAssetOperatingSystem.LINUX,
                    arch = ModelAssetArchitecture.X86_64,
                    archive = ModelAssetArchive(
                        format = ModelAssetArchiveFormat.ZIP,
                        executableName = "llama-server.exe",
                    ),
                ),
            ),
        )

        Files.readString(executable) shouldBe "server"
        Files.readString(executable.parent.resolve("libllama.dll")) shouldBe "dll"
        Files.exists(executable.parent.resolve("ignored.txt")) shouldBe false
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

    private fun createZipArchive(
        archiveFile: Path,
        entries: Map<String, String>,
    ) {
        ZipOutputStream(Files.newOutputStream(archiveFile)).use { output ->
            entries.forEach { (name, content) ->
                output.putNextEntry(ZipEntry(name))
                output.write(content.toByteArray())
                output.closeEntry()
            }
        }
    }
}
