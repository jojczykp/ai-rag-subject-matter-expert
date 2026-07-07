package org.alterbit.aisme.chat.embedded

import jakarta.annotation.PreDestroy
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.alterbit.aisme.chatmodel.ChatModelAvailability
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class LlamaRuntimeProcessManager(
    chatModelRegistry: ChatModelRegistry,
    llamaRuntimeProperties: LlamaRuntimeProperties,
    portAllocator: EphemeralLlamaRuntimePortAllocator,
    private val processLauncher: LlamaRuntimeProcessLauncher,
    private val readinessProbe: LlamaServerReadinessProbe,
    private val processOutputLogger: LlamaRuntimeProcessOutputLogger,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val managedModels: List<ManagedLlamaRuntimeModel> =
        buildManagedModels(
            chatModelRegistry = chatModelRegistry,
            llamaRuntimeProperties = llamaRuntimeProperties,
            portAllocator = portAllocator,
        )
    private val availabilityByModelId = ConcurrentHashMap(
        managedModels.associate { it.modelId to ChatModelAvailability.CONFIGURED },
    )
    private val runningProcesses = mutableListOf<Process>()

    fun baseUrlForModelId(modelId: String): String? =
        managedModels.firstOrNull { it.modelId == modelId }?.baseUrl

    fun availabilityForModelId(modelId: String): ChatModelAvailability =
        availabilityByModelId[modelId] ?: ChatModelAvailability.MISCONFIGURED

    override fun run(args: ApplicationArguments) {
        managedModels.forEach { managedModel ->
            logger.info("Starting managed llama-server process for model '{}'", managedModel.modelId)

            val process = processLauncher.start(managedModel.command)
            runningProcesses += process
            processOutputLogger.attach(managedModel.modelId, process)
            if (readinessProbe.awaitReady(managedModel.baseUrl, STARTUP_TIMEOUT)) {
                availabilityByModelId[managedModel.modelId] = ChatModelAvailability.AVAILABLE
                logger.info("Managed llama-server process for model '{}' is ready", managedModel.modelId)
            } else {
                availabilityByModelId[managedModel.modelId] = ChatModelAvailability.UNAVAILABLE
                logger.warn("Managed llama-server process for model '{}' did not become ready", managedModel.modelId)
                if (process.isAlive) {
                    process.destroy()
                }
            }
        }
    }

    @PreDestroy
    fun stop() {
        runningProcesses.forEach { process ->
            if (process.isAlive) {
                logger.info("Stopping managed llama-server process")
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
        val STARTUP_TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
