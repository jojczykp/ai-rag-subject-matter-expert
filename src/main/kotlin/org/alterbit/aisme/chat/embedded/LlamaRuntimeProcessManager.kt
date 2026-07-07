package org.alterbit.aisme.chat.embedded

import jakarta.annotation.PreDestroy
import java.nio.file.Path
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class LlamaRuntimeProcessManager(
    chatModelRegistry: ChatModelRegistry,
    llamaRuntimeProperties: LlamaRuntimeProperties,
    portAllocator: EphemeralLlamaRuntimePortAllocator,
    private val processLauncher: LlamaRuntimeProcessLauncher,
) : ApplicationRunner {
    private val managedModels: List<ManagedLlamaRuntimeModel> =
        buildManagedModels(
            chatModelRegistry = chatModelRegistry,
            llamaRuntimeProperties = llamaRuntimeProperties,
            portAllocator = portAllocator,
        )
    private val runningProcesses = mutableListOf<Process>()

    fun baseUrlForModelId(modelId: String): String? =
        managedModels.firstOrNull { it.modelId == modelId }?.baseUrl

    override fun run(args: ApplicationArguments) {
        managedModels.forEach { managedModel ->
            runningProcesses += processLauncher.start(managedModel.command)
        }
    }

    @PreDestroy
    fun stop() {
        runningProcesses.forEach { process ->
            if (process.isAlive) {
                process.destroy()
            }
        }
    }

    private fun buildManagedModels(
        chatModelRegistry: ChatModelRegistry,
        llamaRuntimeProperties: LlamaRuntimeProperties,
        portAllocator: EphemeralLlamaRuntimePortAllocator,
    ): List<ManagedLlamaRuntimeModel> {
        if (!llamaRuntimeProperties.enabled) {
            return emptyList()
        }

        val config = llamaRuntimeProperties.requireEnabledConfig()
        val runtimeModelsById = config.models.associateBy { it.id }
        return chatModelRegistry.chatModels()
            .filter { it.runtime == ChatModelRuntime.EMBEDDED_OFFLINE }
            .mapNotNull { chatModel ->
                runtimeModelsById[chatModel.id]?.let { runtimeModel ->
                    val port = portAllocator.allocate()
                    ManagedLlamaRuntimeModel(
                        modelId = chatModel.id,
                        baseUrl = "http://$LOOPBACK_HOST:$port",
                        command = config.commandFor(runtimeModel, port),
                    )
                }
            }
    }

    private fun EnabledLlamaRuntimeProperties.commandFor(
        model: LlamaRuntimeModelProperties,
        port: Int,
    ): List<String> =
        listOf(
            serverExecutablePath,
            "--host",
            LOOPBACK_HOST,
            "--port",
            port.toString(),
            "--model",
            Path.of(assetDirectory).resolveConfiguredPath(model.ggufFile).toString(),
            "--ctx-size",
            model.contextSize.toString(),
        ) + model.runtimeArguments

    private fun Path.resolveConfiguredPath(path: String): Path {
        val configuredPath = Path.of(path)
        return if (configuredPath.isAbsolute) configuredPath else resolve(configuredPath)
    }

    private data class ManagedLlamaRuntimeModel(
        val modelId: String,
        val baseUrl: String,
        val command: List<String>,
    )

    private companion object {
        const val LOOPBACK_HOST = "127.0.0.1"
    }
}
