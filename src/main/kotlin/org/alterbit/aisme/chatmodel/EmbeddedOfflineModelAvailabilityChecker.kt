package org.alterbit.aisme.chatmodel

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.alterbit.aisme.chat.embedded.LlamaRuntimeProperties
import org.springframework.stereotype.Component

@Component
class EmbeddedOfflineModelAvailabilityChecker(
    private val llamaRuntimeProperties: LlamaRuntimeProperties,
) : ChatModelAvailabilityChecker {
    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.EMBEDDED_OFFLINE

    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability {
        if (model.mode != ChatModelMode.EMBEDDED_OFFLINE || !model.availableOffline) {
            return ChatModelAvailability.MISCONFIGURED
        }

        if (!llamaRuntimeProperties.enabled) {
            return ChatModelAvailability.MISCONFIGURED
        }

        val config = llamaRuntimeProperties.config ?: return ChatModelAvailability.MISCONFIGURED
        val runtimeModel = config.models.find { it.id == model.id } ?: return ChatModelAvailability.MISCONFIGURED
        val assetDirectory = Path.of(config.assetDirectory)
        val ggufFile = assetDirectory.resolveConfiguredPath(runtimeModel.ggufFile)
        val serverExecutable = Path.of(config.serverExecutablePath)

        return if (
            Files.isDirectory(assetDirectory) &&
            Files.isReadable(assetDirectory) &&
            Files.isRegularFile(ggufFile) &&
            Files.isReadable(ggufFile) &&
            Files.isRegularFile(serverExecutable) &&
            Files.isExecutable(serverExecutable)
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
}
