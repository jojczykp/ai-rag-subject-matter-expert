package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.alterbit.aisme.modelcatalog.ChatModelAvailability
import org.alterbit.aisme.modelcatalog.ChatModelMode
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelProperties
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelsProperties
import org.alterbit.aisme.modelcatalog.EnabledChatModelProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments

class EmbeddedLlamaProcessManagerTest {
    @Test
    fun `does not start processes for disabled embedded llama models`() {
        val launcher = FakeEmbeddedLlamaProcessLauncher()
        val manager = EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-llama")),
            embeddedLlamaProperties = embeddedLlamaProperties(runtimeModel(id = "embedded-llama", enabled = false)),
            portAllocator = fixedPortAllocator(19001),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(),
            processOutputLogger = noOpOutputLogger(),
        )

        manager.run(DefaultApplicationArguments())

        launcher.commands shouldContainExactly emptyList()
        manager.baseUrlForModelId("embedded-llama") shouldBe null
        manager.availabilityForModelId("embedded-llama") shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `starts one llama server process per configured embedded model`() {
        val launcher = FakeEmbeddedLlamaProcessLauncher()
        val manager = EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "embedded-llama"),
                ollamaModel(id = "local-llama"),
                embeddedModel(id = "embedded-qwen"),
            ),
            embeddedLlamaProperties = embeddedLlamaProperties(
                runtimeModel(
                    id = "embedded-llama",
                    enabled = true,
                    ggufFile = "llama.gguf",
                    contextSize = 4096,
                    runtimeArguments = listOf("--threads", "8"),
                ),
                runtimeModel(
                    id = "embedded-qwen",
                    enabled = true,
                    ggufFile = "/opt/models/qwen.gguf",
                    contextSize = 8192,
                ),
            ),
            portAllocator = fixedPortAllocator(19001, 19002),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(),
            processOutputLogger = noOpOutputLogger(),
        )

        manager.run(DefaultApplicationArguments())

        manager.baseUrlForModelId("embedded-llama") shouldBe "http://127.0.0.1:19001"
        manager.baseUrlForModelId("embedded-qwen") shouldBe "http://127.0.0.1:19002"
        manager.availabilityForModelId("embedded-llama") shouldBe ChatModelAvailability.AVAILABLE
        manager.availabilityForModelId("embedded-qwen") shouldBe ChatModelAvailability.AVAILABLE
        launcher.commands shouldContainExactly listOf(
            listOf(
                "./models/llama/bin/llama-server",
                "--host",
                "127.0.0.1",
                "--port",
                "19001",
                "--model",
                "./models/llama/llama.gguf",
                "--ctx-size",
                "4096",
                "--threads",
                "8",
            ),
            listOf(
                "./models/llama/bin/llama-server",
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
    fun `skips embedded chat model without matching runtime model`() {
        val launcher = FakeEmbeddedLlamaProcessLauncher()
        val manager = EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "configured-runtime-model"),
                embeddedModel(id = "missing-runtime-model"),
            ),
            embeddedLlamaProperties = embeddedLlamaProperties(runtimeModel(id = "configured-runtime-model", enabled = true)),
            portAllocator = fixedPortAllocator(19001),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(),
            processOutputLogger = noOpOutputLogger(),
        )

        manager.run(DefaultApplicationArguments())

        manager.baseUrlForModelId("configured-runtime-model") shouldBe "http://127.0.0.1:19001"
        manager.baseUrlForModelId("missing-runtime-model") shouldBe null
        launcher.commands.single()[4] shouldBe "19001"
    }

    @Test
    fun `stops running llama server processes`() {
        val launcher = FakeEmbeddedLlamaProcessLauncher()
        val manager = EmbeddedLlamaProcessManager(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-llama")),
            embeddedLlamaProperties = embeddedLlamaProperties(runtimeModel(id = "embedded-llama", enabled = true)),
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
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-llama")),
            embeddedLlamaProperties = embeddedLlamaProperties(runtimeModel(id = "embedded-llama", enabled = true)),
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
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-llama")),
            embeddedLlamaProperties = embeddedLlamaProperties(runtimeModel(id = "embedded-llama", enabled = true)),
            portAllocator = fixedPortAllocator(19001),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(ready = false),
            processOutputLogger = noOpOutputLogger(),
        )

        manager.run(DefaultApplicationArguments())

        manager.availabilityForModelId("embedded-llama") shouldBe ChatModelAvailability.UNAVAILABLE
        launcher.processes.single().destroyed shouldBe true
    }

    private fun chatModelRegistry(vararg models: ConfiguredChatModelProperties): ChatModelRegistry =
        ChatModelRegistry(ConfiguredChatModelsProperties(chatModels = models.toList()))

    private fun embeddedModel(id: String): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            enabled = true,
            config = EnabledChatModelProperties(
                displayName = "Embedded Llama",
                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                mode = ChatModelMode.EMBEDDED_OFFLINE,
                availableOffline = true,
            ),
        )

    private fun ollamaModel(id: String): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            enabled = true,
            config = EnabledChatModelProperties(
                displayName = "Local Llama",
                runtime = ChatModelRuntime.OLLAMA,
                mode = ChatModelMode.LOCAL_SERVER,
                availableOffline = false,
                baseUrl = "http://localhost:11434",
                modelName = "llama3.2",
            ),
        )

    private fun embeddedLlamaProperties(
        vararg models: EmbeddedLlamaModelProperties,
    ): EmbeddedLlamaProperties =
        EmbeddedLlamaProperties(
            assetDirectory = "./models/llama",
            serverExecutablePath = "./models/llama/bin/llama-server",
            models = models.toList(),
        )

    private fun runtimeModel(
        id: String,
        enabled: Boolean = false,
        ggufFile: String = "llama.gguf",
        contextSize: Int = 4096,
        runtimeArguments: List<String> = emptyList(),
    ): EmbeddedLlamaModelProperties =
        EmbeddedLlamaModelProperties(
            id = id,
            enabled = enabled,
            displayName = "Embedded Llama",
            ggufFile = ggufFile,
            contextSize = contextSize,
            runtimeArguments = runtimeArguments,
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
        override fun awaitReady(baseUrl: String, timeout: Duration): Boolean =
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

        override fun waitFor(timeout: Long, unit: TimeUnit): Boolean =
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
