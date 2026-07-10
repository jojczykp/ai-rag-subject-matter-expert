package org.alterbit.aisme.chatmodel

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProcessManager
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaModelProperties
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProperties
import org.springframework.stereotype.Component

@Component
class EmbeddedOfflineModelAvailabilityChecker(
    embeddedLlamaProperties: EmbeddedLlamaProperties,
    private val embeddedLlamaProcessManager: EmbeddedLlamaProcessManager,
) : ChatModelAvailabilityChecker {
    private val staticAvailabilityByModelId: Map<String, ChatModelAvailability> =
        buildStaticAvailabilityByModelId(embeddedLlamaProperties)

    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.EMBEDDED_OFFLINE

    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
        if (model.mode != ChatModelMode.EMBEDDED_OFFLINE || !model.availableOffline) {
            ChatModelAvailability.MISCONFIGURED
        } else if (staticAvailabilityByModelId[model.id] != ChatModelAvailability.AVAILABLE) {
            staticAvailabilityByModelId[model.id] ?: ChatModelAvailability.MISCONFIGURED
        } else {
            embeddedLlamaProcessManager.availabilityForModelId(model.id)
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
            ggufFile.matchesConfiguredChecksum(runtimeModel.sha256)
        ) {
            ChatModelAvailability.AVAILABLE
        } else {
            ChatModelAvailability.MISCONFIGURED
        }
    }

    private fun Path.resolveConfiguredPath(path: String): Path {
        val configuredPath = Path.of(path)
        return if (configuredPath.isAbsolute) configuredPath else resolve(configuredPath)
    }

    private fun Path.matchesConfiguredChecksum(expectedSha256: String?): Boolean =
        expectedSha256 == null || expectedSha256 == sha256()

    private fun Path.sha256(): String? =
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(this).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
            digest.digest().joinToString(separator = "") { "%02x".format(it) }
        } catch (_: IOException) {
            null
        }
}
