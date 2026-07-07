package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration
import org.alterbit.aisme.chatmodel.ChatModelAvailability
import org.alterbit.aisme.chatmodel.ChatModelMode
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.alterbit.aisme.chatmodel.ConfiguredChatModelProperties
import org.alterbit.aisme.chatmodel.ConfiguredChatModelsProperties
import org.alterbit.aisme.chatmodel.EnabledChatModelProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments

class LlamaRuntimeProcessManagerTest {
    @Test
    fun `does not start processes when llama runtime is disabled`() {
        val launcher = FakeLlamaRuntimeProcessLauncher()
        val manager = LlamaRuntimeProcessManager(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-llama")),
            llamaRuntimeProperties = LlamaRuntimeProperties(enabled = false),
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
        val launcher = FakeLlamaRuntimeProcessLauncher()
        val manager = LlamaRuntimeProcessManager(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "embedded-llama"),
                ollamaModel(id = "local-llama"),
                embeddedModel(id = "embedded-qwen"),
            ),
            llamaRuntimeProperties = enabledLlamaRuntimeProperties(
                runtimeModel(
                    id = "embedded-llama",
                    ggufFile = "llama.gguf",
                    contextSize = 4096,
                    runtimeArguments = listOf("--threads", "8"),
                ),
                runtimeModel(
                    id = "embedded-qwen",
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
        val launcher = FakeLlamaRuntimeProcessLauncher()
        val manager = LlamaRuntimeProcessManager(
            chatModelRegistry = chatModelRegistry(
                embeddedModel(id = "configured-runtime-model"),
                embeddedModel(id = "missing-runtime-model"),
            ),
            llamaRuntimeProperties = enabledLlamaRuntimeProperties(runtimeModel(id = "configured-runtime-model")),
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
        val launcher = FakeLlamaRuntimeProcessLauncher()
        val manager = LlamaRuntimeProcessManager(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-llama")),
            llamaRuntimeProperties = enabledLlamaRuntimeProperties(runtimeModel(id = "embedded-llama")),
            portAllocator = fixedPortAllocator(19001),
            processLauncher = launcher,
            readinessProbe = FakeReadinessProbe(),
            processOutputLogger = noOpOutputLogger(),
        )

        manager.run(DefaultApplicationArguments())
        manager.stop()

        launcher.processes.single().destroyed shouldBe true
    }

    @Test
    fun `marks model unavailable and stops process when readiness fails`() {
        val launcher = FakeLlamaRuntimeProcessLauncher()
        val manager = LlamaRuntimeProcessManager(
            chatModelRegistry = chatModelRegistry(embeddedModel(id = "embedded-llama")),
            llamaRuntimeProperties = enabledLlamaRuntimeProperties(runtimeModel(id = "embedded-llama")),
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

    private fun enabledLlamaRuntimeProperties(
        vararg models: LlamaRuntimeModelProperties,
    ): LlamaRuntimeProperties =
        LlamaRuntimeProperties(
            enabled = true,
            config = EnabledLlamaRuntimeProperties(
                assetDirectory = "./models/llama",
                serverExecutablePath = "./models/llama/bin/llama-server",
                models = models.toList(),
            ),
        )

    private fun runtimeModel(
        id: String,
        ggufFile: String = "llama.gguf",
        contextSize: Int = 4096,
        runtimeArguments: List<String> = emptyList(),
    ): LlamaRuntimeModelProperties =
        LlamaRuntimeModelProperties(
            id = id,
            displayName = "Embedded Llama",
            ggufFile = ggufFile,
            contextSize = contextSize,
            runtimeArguments = runtimeArguments,
        )

    private fun fixedPortAllocator(vararg ports: Int): EphemeralLlamaRuntimePortAllocator {
        val remainingPorts = ports.toMutableList()
        return EphemeralLlamaRuntimePortAllocator { remainingPorts.removeFirst() }
    }

    private fun noOpOutputLogger(): LlamaRuntimeProcessOutputLogger =
        LlamaRuntimeProcessOutputLogger(lineConsumer = { _, _, _ -> })

    private class FakeLlamaRuntimeProcessLauncher : LlamaRuntimeProcessLauncher {
        val commands = mutableListOf<List<String>>()
        val processes = mutableListOf<FakeProcess>()

        override fun start(command: List<String>): Process {
            commands += command
            return FakeProcess().also { processes += it }
        }
    }

    private class FakeReadinessProbe(
        private val ready: Boolean = true,
    ) : LlamaServerReadinessProbe {
        override fun awaitReady(baseUrl: String, timeout: Duration): Boolean =
            ready
    }

    private class FakeProcess : Process() {
        var destroyed: Boolean = false

        override fun getOutputStream(): OutputStream =
            ByteArrayOutputStream()

        override fun getInputStream(): InputStream =
            ByteArrayInputStream(ByteArray(0))

        override fun getErrorStream(): InputStream =
            ByteArrayInputStream(ByteArray(0))

        override fun waitFor(): Int =
            0

        override fun exitValue(): Int =
            if (destroyed) 0 else throw IllegalThreadStateException()

        override fun destroy() {
            destroyed = true
        }

        override fun isAlive(): Boolean =
            !destroyed
    }
}
