package org.alterbit.aisme.modelcatalog

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProcessManager
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaModelProperties
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmbeddedOfflineModelAvailabilityChecker(
    embeddedLlamaProperties: EmbeddedLlamaProperties,
    private val embeddedLlamaProcessManager: EmbeddedLlamaProcessManager,
) : ChatModelAvailabilityChecker {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val staticAvailabilityByModelId: Map<String, ChatModelAvailability> =
        buildStaticAvailabilityByModelId(embeddedLlamaProperties)

    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.EMBEDDED_OFFLINE

    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
        if (model.mode != ChatModelMode.EMBEDDED_OFFLINE || !model.availableOffline) {
            logger.warn("Embedded model '{}' is misconfigured for offline availability checks", model.id)
            ChatModelAvailability.MISCONFIGURED
        } else if (staticAvailabilityByModelId[model.id] != ChatModelAvailability.AVAILABLE) {
            (staticAvailabilityByModelId[model.id] ?: ChatModelAvailability.MISCONFIGURED).also {
                logger.warn("Embedded model '{}' static asset availability is '{}'", model.id, it)
            }
        } else {
            embeddedLlamaProcessManager.availabilityForModelId(model.id).also {
                logger.info("Embedded model '{}' runtime availability is '{}'", model.id, it)
            }
        }

    private fun buildStaticAvailabilityByModelId(
        embeddedLlamaProperties: EmbeddedLlamaProperties,
    ): Map<String, ChatModelAvailability> {
        return embeddedLlamaProperties.enabledModels().associate { runtimeModel ->
            runtimeModel.id to embeddedLlamaProperties.availabilityFor(runtimeModel)
        }
    }

    private fun EmbeddedLlamaProperties.availabilityFor(
        runtimeModel: EmbeddedLlamaModelProperties,
    ): ChatModelAvailability {
        val assetDirectory = Path.of(assetDirectory)
        val ggufFile = assetDirectory.resolveConfiguredPath(runtimeModel.ggufFile)
        val serverExecutable = Path.of(serverExecutablePath)

        return if (
            Files.isDirectory(assetDirectory) &&
            Files.isReadable(assetDirectory) &&
            Files.isRegularFile(ggufFile) &&
            Files.isReadable(ggufFile) &&
            Files.isRegularFile(serverExecutable) &&
            Files.isExecutable(serverExecutable) &&
            serverExecutable.canBeOpened()
        ) {
            logger.info("Embedded model '{}' static assets are available", runtimeModel.id)
            ChatModelAvailability.AVAILABLE
        } else {
            logger.warn("Embedded model '{}' static assets are misconfigured or unavailable", runtimeModel.id)
            ChatModelAvailability.MISCONFIGURED
        }
    }

    private fun Path.resolveConfiguredPath(path: String): Path {
        val configuredPath = Path.of(path)
        return if (configuredPath.isAbsolute) configuredPath else resolve(configuredPath)
    }

    private fun Path.canBeOpened(): Boolean =
        try {
            Files.newInputStream(this).use { input ->
                input.read()
            }
            true
        } catch (ex: IOException) {
            logger.warn(
                "Could not open embedded llama-server executable '{}': '{}'",
                this,
                ex.javaClass.simpleName,
            )
            false
        }
}
