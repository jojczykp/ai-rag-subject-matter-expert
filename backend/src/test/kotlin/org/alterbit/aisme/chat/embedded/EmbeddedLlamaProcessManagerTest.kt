package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.alterbit.aisme.modelcatalog.ChatModelAvailability
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.ChatModelProperties
import org.alterbit.aisme.modelcatalog.ChatModelRuntimeProperties
import org.alterbit.aisme.modelcatalog.ChatModelsProperties
import org.alterbit.aisme.modelcatalog.ChatModelRuntimeConfigProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments

class EmbeddedLlamaProcessManagerTest {
    @Test
    fun `does not start processes for disabled embedded llama models`() {
        val launcher = FakeEmbeddedLlamaProcessLauncher()
        val manager = EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-qwen", enabled = false)),
            portAllocator = fixedPortAllocator(19001),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(),
            processOutputLogger = noOpOutputLogger(),
        )
        manager.run(DefaultApplicationArguments())

        launcher.commands shouldContainExactly emptyList()
        manager.baseUrlForModelId("embedded-qwen") shouldBe null
        manager.availabilityForModelId("embedded-qwen") shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `starts one llama server process per configured embedded model`() {
        val launcher = FakeEmbeddedLlamaProcessLauncher()
        val manager = EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(
                    id = "embedded-qwen",
                    displayOrder = 10,
                    ggufFile = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                    contextSize = 2048,
                    runtimeArguments = listOf("--threads", "8"),
                ),
                ollamaModel(id = "local-llama"),
                embeddedModel(
                    id = "embedded-mistral",
                    displayOrder = 20,
                    ggufFile = "/opt/models/qwen.gguf",
                    contextSize = 8192,
                ),
            ),
            portAllocator = fixedPortAllocator(19001, 19002),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(),
            processOutputLogger = noOpOutputLogger(),
        )
        val serverExecutablePath = Path.of("./models/llama/bin/llama-server").toAbsolutePath().normalize().toString()
        val llamaModelPath = Path.of("./models/llama/qwen2.5-0.5b-instruct-q4_k_m.gguf").toAbsolutePath().normalize().toString()

        manager.run(DefaultApplicationArguments())

        manager.baseUrlForModelId("embedded-qwen") shouldBe "http://127.0.0.1:19001"
        manager.baseUrlForModelId("embedded-mistral") shouldBe "http://127.0.0.1:19002"
        manager.availabilityForModelId("embedded-qwen") shouldBe ChatModelAvailability.AVAILABLE
        manager.availabilityForModelId("embedded-mistral") shouldBe ChatModelAvailability.AVAILABLE
        launcher.commands shouldContainExactly listOf(
            listOf(
                serverExecutablePath,
                "--host",
                "127.0.0.1",
                "--port",
                "19001",
                "--model",
                llamaModelPath,
                "--ctx-size",
                "2048",
                "--threads",
                "8",
            ),
            listOf(
                serverExecutablePath,
                "--host",
                "127.0.0.1",
                "--port",
                "19002",
                "--model",
                "/opt/models/qwen.gguf",
                "--ctx-size",
                "8192",
            ),
        )
    }

    @Test
    fun `stops running llama server processes`() {
        val launcher = FakeEmbeddedLlamaProcessLauncher()
        val manager = EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-qwen")),
            portAllocator = fixedPortAllocator(19001),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(),
            processOutputLogger = noOpOutputLogger(),
        )

        manager.run(DefaultApplicationArguments())
        manager.stop()

        launcher.processes.single().destroyed shouldBe true
        launcher.processes.single().forciblyDestroyed shouldBe false
    }

    @Test
    fun `force stops running llama server process when graceful stop times out`() {
        val launcher = FakeEmbeddedLlamaProcessLauncher(processStopsGracefully = false)
        val manager = EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-qwen")),
            portAllocator = fixedPortAllocator(19001),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(),
            processOutputLogger = noOpOutputLogger(),
        )

        manager.run(DefaultApplicationArguments())
        manager.stop()

        launcher.processes.single().destroyed shouldBe true
        launcher.processes.single().forciblyDestroyed shouldBe true
    }

    @Test
    fun `marks model unavailable and stops process when readiness fails`() {
        val launcher = FakeEmbeddedLlamaProcessLauncher()
        val manager = EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-qwen")),
            portAllocator = fixedPortAllocator(19001),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(ready = false),
            processOutputLogger = noOpOutputLogger(),
        )

        manager.run(DefaultApplicationArguments())

        manager.availabilityForModelId("embedded-qwen") shouldBe ChatModelAvailability.UNAVAILABLE
        launcher.processes.single().destroyed shouldBe true
    }

    private fun chatModelRegistry(vararg models: Pair<String, ChatModelProperties>): ChatModelRegistry =
        ChatModelRegistry(
            ChatModelsProperties(
                chatRuntimesById = mapOf(
                    "embedded-llama" to ChatModelRuntimeConfigProperties(
                        type = ChatModelRuntime.EMBEDDED_LLAMA,
                        assetDirectory = "./models/llama",
                        serverExecutablePath = "./models/llama/bin/llama-server",
                    ),
                    "local-ollama" to ChatModelRuntimeConfigProperties(
                        type = ChatModelRuntime.OLLAMA,
                        baseUrl = "http://localhost:11434",
                    ),
                ),
                chatModelsById = models.toMap().withFallbackModelWhenNoneEnabled(),
            ),
        )

    private fun Map<String, ChatModelProperties>.withFallbackModelWhenNoneEnabled(): Map<String, ChatModelProperties> =
        if (values.any { it.enabled }) this else this + ollamaModel(id = "local-llama")

    private fun embeddedModel(
        id: String,
        enabled: Boolean = true,
        displayOrder: Int? = null,
        ggufFile: String = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
        contextSize: Int = 4096,
        runtimeArguments: List<String> = emptyList(),
    ): Pair<String, ChatModelProperties> =
        id to ChatModelProperties(
            enabled = enabled,
            displayOrder = displayOrder,
            displayName = "Embedded Qwen",
            runtime = ChatModelRuntimeProperties(
                id = "embedded-llama",
                modelName = "qwen2.5",
                ggufFile = ggufFile,
                contextSize = contextSize,
                runtimeArguments = runtimeArguments,
            ),
        )

    private fun ollamaModel(id: String): Pair<String, ChatModelProperties> =
        id to ChatModelProperties(
            enabled = true,
            displayName = "Local Llama",
            runtime = ChatModelRuntimeProperties(
                id = "local-ollama",
                modelName = "llama3.2",
            ),
        )

    private fun fixedPortAllocator(vararg ports: Int): EphemeralEmbeddedLlamaPortAllocator {
        val remainingPorts = ports.toMutableList()
        return EphemeralEmbeddedLlamaPortAllocator { remainingPorts.removeFirst() }
    }

    private fun noOpOutputLogger(): EmbeddedLlamaProcessOutputLogger =
        EmbeddedLlamaProcessOutputLogger(lineConsumer = { _, _, _ -> })

    private class FakeEmbeddedLlamaProcessLauncher(
        private val processStopsGracefully: Boolean = true,
    ) : EmbeddedLlamaProcessLauncher {
        val commands = mutableListOf<List<String>>()
        val processes = mutableListOf<FakeProcess>()

        override fun start(command: List<String>): Process {
            commands += command
            return FakeProcess(processStopsGracefully = processStopsGracefully).also { processes += it }
        }
    }

    private class FakeReadinessProbe(
        private val ready: Boolean = true,
    ) : LlamaServerReadinessProbe {
        override fun awaitReady(baseUrl: String, apiTimeout: Duration): Boolean =
            ready
    }

    private class FakeProcess(
        private val processStopsGracefully: Boolean = true,
    ) : Process() {
        var destroyed: Boolean = false
        var forciblyDestroyed: Boolean = false

        override fun getOutputStream(): OutputStream =
            ByteArrayOutputStream()

        override fun getInputStream(): InputStream =
            ByteArrayInputStream(ByteArray(0))

        override fun getErrorStream(): InputStream =
            ByteArrayInputStream(ByteArray(0))

        override fun pid(): Long =
            12345L

        override fun waitFor(): Int =
            0

        override fun waitFor(apiTimeout: Long, unit: TimeUnit): Boolean =
            if (processStopsGracefully) {
                true
            } else {
                false
            }

        override fun exitValue(): Int =
            if (!isAlive) 0 else throw IllegalThreadStateException()

        override fun destroy() {
            destroyed = true
        }

        override fun destroyForcibly(): Process {
            forciblyDestroyed = true
            return this
        }

        override fun isAlive(): Boolean =
            if (processStopsGracefully) {
                !destroyed
            } else {
                !forciblyDestroyed
            }
    }
}
