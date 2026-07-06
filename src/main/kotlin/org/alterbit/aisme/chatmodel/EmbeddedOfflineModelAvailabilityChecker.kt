package org.alterbit.aisme.chatmodel

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.alterbit.aisme.chat.embedded.EnabledLlamaRuntimeProperties
import org.alterbit.aisme.chat.embedded.LlamaRuntimeModelProperties
import org.alterbit.aisme.chat.embedded.LlamaRuntimeProperties
import org.springframework.stereotype.Component

@Component
class EmbeddedOfflineModelAvailabilityChecker(
    llamaRuntimeProperties: LlamaRuntimeProperties,
) : ChatModelAvailabilityChecker {
    private val availabilityByModelId: Map<String, ChatModelAvailability> =
        buildAvailabilityByModelId(llamaRuntimeProperties)

    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.EMBEDDED_OFFLINE

    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
        if (model.mode != ChatModelMode.EMBEDDED_OFFLINE || !model.availableOffline) {
            ChatModelAvailability.MISCONFIGURED
        } else {
            availabilityByModelId[model.id] ?: ChatModelAvailability.MISCONFIGURED
        }

    private fun buildAvailabilityByModelId(
        llamaRuntimeProperties: LlamaRuntimeProperties,
    ): Map<String, ChatModelAvailability> {
        if (!llamaRuntimeProperties.enabled) {
            return emptyMap()
        }

        val config = llamaRuntimeProperties.config ?: return emptyMap()
        return config.models.associate { runtimeModel ->
            runtimeModel.id to config.availabilityFor(runtimeModel)
        }
    }

    private fun EnabledLlamaRuntimeProperties.availabilityFor(
        runtimeModel: LlamaRuntimeModelProperties,
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
