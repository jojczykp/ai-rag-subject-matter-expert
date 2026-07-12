package org.alterbit.aisme.chat.embedded

import jakarta.annotation.PreDestroy
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import org.alterbit.aisme.modelcatalog.ChatModelAvailability
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class EmbeddedLlamaProcessManager(
    chatModelRegistry: ChatModelRegistry,
    embeddedLlamaProperties: EmbeddedLlamaProperties,
    portAllocator: EphemeralEmbeddedLlamaPortAllocator,
    private val processLauncher: EmbeddedLlamaProcessLauncher,
    private val readinessProbe: LlamaServerReadinessProbe,
    private val processOutputLogger: EmbeddedLlamaProcessOutputLogger,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val managedModels: List<ManagedEmbeddedLlamaModel> =
        buildManagedModels(
            chatModelRegistry = chatModelRegistry,
            embeddedLlamaProperties = embeddedLlamaProperties,
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
            logger.info(
                "Starting managed llama-server process for model '{}' on port '{}'",
                managedModel.modelId,
                managedModel.port,
            )

            val process = processLauncher.start(managedModel.command)
            logger.info(
                "Started managed llama-server process for model '{}' with pid '{}'",
                managedModel.modelId,
                process.pid(),
            )
            runningProcesses += process
            processOutputLogger.attach(managedModel.modelId, process)
            if (readinessProbe.awaitReady(managedModel.baseUrl, STARTUP_TIMEOUT)) {
                availabilityByModelId[managedModel.modelId] = ChatModelAvailability.AVAILABLE
                logger.info("Managed llama-server process for model '{}' is ready", managedModel.modelId)
            } else {
                availabilityByModelId[managedModel.modelId] = ChatModelAvailability.UNAVAILABLE
                logger.warn("Managed llama-server process for model '{}' did not become ready within {}", managedModel.modelId, STARTUP_TIMEOUT)
                stopProcess(process)
            }
        }
    }

    @PreDestroy
    fun stop() {
        runningProcesses.forEach { process ->
            stopProcess(process)
        }
    }

    private fun stopProcess(process: Process) {
        if (!process.isAlive) {
            return
        }

        logger.info("Stopping managed llama-server process")
        process.destroy()

        if (process.waitFor(GRACEFUL_SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS) || !process.isAlive) {
            logger.info("Managed llama-server process with pid '{}' stopped gracefully", process.pid())
        } else {
            logger.warn("Managed llama-server process did not stop gracefully; forcing shutdown")
            process.destroyForcibly()
            logger.info("Managed llama-server process with pid '{}' was force stopped", process.pid())
        }
    }

    private fun buildManagedModels(
        chatModelRegistry: ChatModelRegistry,
        embeddedLlamaProperties: EmbeddedLlamaProperties,
        portAllocator: EphemeralEmbeddedLlamaPortAllocator,
    ): List<ManagedEmbeddedLlamaModel> {
        val runtimeModelsById = embeddedLlamaProperties.enabledModels().associateBy { it.id }

        return chatModelRegistry.chatModels()
            .filter { it.runtime == ChatModelRuntime.EMBEDDED_OFFLINE }
            .mapNotNull { chatModel ->
                runtimeModelsById[chatModel.id]?.let { runtimeModel ->
                    val port = portAllocator.allocate()
                    ManagedEmbeddedLlamaModel(
                        modelId = chatModel.id,
                        port = port,
                        baseUrl = "http://$LOOPBACK_HOST:$port",
                        command = embeddedLlamaProperties.commandFor(runtimeModel, port),
                    )
                }
            }
    }

    private fun EmbeddedLlamaProperties.commandFor(
        model: EmbeddedLlamaModelProperties,
        port: Int,
    ): List<String> =
        listOf(
            Path.of(serverExecutablePath).absoluteNormalizedPath(),
            "--host",
            LOOPBACK_HOST,
            "--port",
            port.toString(),
            "--model",
            Path.of(assetDirectory).resolveConfiguredPath(model.ggufFile).absoluteNormalizedPath(),
            "--ctx-size",
            model.contextSize.toString(),
        ) + model.runtimeArguments

    private fun Path.resolveConfiguredPath(path: String): Path {
        val configuredPath = Path.of(path)
        return if (configuredPath.isAbsolute) configuredPath else resolve(configuredPath)
    }

    private fun Path.absoluteNormalizedPath(): String =
        toAbsolutePath().normalize().toString()

    private data class ManagedEmbeddedLlamaModel(
        val modelId: String,
        val port: Int,
        val baseUrl: String,
        val command: List<String>,
    )

    private companion object {
        const val LOOPBACK_HOST = "127.0.0.1"
        val STARTUP_TIMEOUT: Duration = Duration.ofSeconds(30)
        val GRACEFUL_SHUTDOWN_TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}
