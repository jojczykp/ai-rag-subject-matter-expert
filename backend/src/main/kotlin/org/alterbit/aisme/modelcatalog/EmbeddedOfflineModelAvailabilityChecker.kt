package org.alterbit.aisme.modelcatalog

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProcessManager
import org.alterbit.aisme.chat.embedded.requireEmbeddedAssetDirectory
import org.alterbit.aisme.chat.embedded.requireEmbeddedGgufFile
import org.alterbit.aisme.chat.embedded.requireEmbeddedServerExecutablePath
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmbeddedOfflineModelAvailabilityChecker(
    private val embeddedLlamaProcessManager: EmbeddedLlamaProcessManager,
) : ChatModelAvailabilityChecker {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.EMBEDDED_OFFLINE

    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
        if (model.mode != ChatModelMode.EMBEDDED_OFFLINE || !model.availableOffline) {
            logger.warn("Embedded model '{}' is misconfigured for offline availability checks", model.id)
            ChatModelAvailability.MISCONFIGURED
        } else {
            val staticAvailability = model.staticAssetAvailability()
            if (staticAvailability != ChatModelAvailability.AVAILABLE) {
                logger.warn("Embedded model '{}' static asset availability is '{}'", model.id, staticAvailability)
                staticAvailability
            } else {
                embeddedLlamaProcessManager.availabilityForModelId(model.id).also {
                    logger.info("Embedded model '{}' runtime availability is '{}'", model.id, it)
                }
            }
        }

    private fun ChatModelDescriptor.staticAssetAvailability(): ChatModelAvailability {
        val assetDirectory = Path.of(requireEmbeddedAssetDirectory())
        val ggufFile = assetDirectory.resolveConfiguredPath(requireEmbeddedGgufFile())
        val serverExecutable = Path.of(requireEmbeddedServerExecutablePath())

        val assetProblems = listOfNotNull(
            assetDirectory.problemIfNotReadableDirectory("asset directory"),
            ggufFile.problemIfNotReadableFile("GGUF model file"),
            serverExecutable.problemIfNotExecutableFile("llama-server executable"),
            serverExecutable.problemIfCannotBeOpened("llama-server executable"),
        )

        return if (assetProblems.isEmpty()) {
            logger.info("Embedded model '{}' static assets are available", id)
            ChatModelAvailability.AVAILABLE
        } else {
            assetProblems.forEach { problem ->
                logger.warn("Embedded model '{}' {}", id, problem)
            }
            ChatModelAvailability.MISCONFIGURED
        }
    }

    private fun Path.resolveConfiguredPath(path: String): Path {
        val configuredPath = Path.of(path)
        return if (configuredPath.isAbsolute) configuredPath else resolve(configuredPath)
    }

    private fun Path.problemIfNotReadableDirectory(label: String): String? =
        when {
            !Files.exists(this) -> "$label not found: $this"
            !Files.isDirectory(this) -> "$label is not a directory: $this"
            !Files.isReadable(this) -> "$label is not readable: $this"
            else -> null
        }

    private fun Path.problemIfNotReadableFile(label: String): String? =
        when {
            !Files.exists(this) -> "$label not found: $this"
            !Files.isRegularFile(this) -> "$label is not a regular file: $this"
            !Files.isReadable(this) -> "$label is not readable: $this"
            else -> null
        }

    private fun Path.problemIfNotExecutableFile(label: String): String? =
        when {
            !Files.exists(this) -> "$label not found: $this"
            !Files.isRegularFile(this) -> "$label is not a regular file: $this"
            !Files.isExecutable(this) -> "$label is not executable: $this"
            else -> null
        }

    private fun Path.problemIfCannotBeOpened(label: String): String? =
        if (!Files.exists(this) || !Files.isRegularFile(this)) {
            null
        } else if (canBeOpened(label)) {
            null
        } else {
            "$label cannot be opened: $this"
        }

    private fun Path.canBeOpened(label: String): Boolean =
        try {
            Files.newInputStream(this).use { input ->
                input.read()
            }
            true
        } catch (ex: IOException) {
            logger.warn(
                "Could not open embedded {} '{}': '{}'",
                label,
                this,
                ex.javaClass.simpleName,
            )
            false
        }
}
