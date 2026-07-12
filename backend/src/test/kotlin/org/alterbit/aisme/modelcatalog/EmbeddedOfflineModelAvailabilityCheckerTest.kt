package org.alterbit.aisme.modelcatalog

import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.alterbit.aisme.chat.embedded.EphemeralEmbeddedLlamaPortAllocator
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaModelProperties
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProcessLauncher
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProcessManager
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProcessOutputLogger
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProperties
import org.alterbit.aisme.chat.embedded.LlamaServerReadinessProbe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.DefaultApplicationArguments

class EmbeddedOfflineModelAvailabilityCheckerTest {
    @field:TempDir
    lateinit var tempDirectory: Path

    @Test
    fun `supports embedded offline runtime`() {
        val checker = checker()

        checker.supports(
            chatModel(
                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                mode = ChatModelMode.EMBEDDED_OFFLINE,
                availableOffline = true,
            ),
        ) shouldBe true
    }

    @Test
    fun `does not support other runtimes`() {
        val checker = checker()

        checker.supports(chatModel(runtime = ChatModelRuntime.OLLAMA)) shouldBe false
    }

    @Test
    fun `marks embedded offline model with existing runtime assets as available`() {
        val assets = runtimeAssets()
        val checker = checker(
            properties = enabledProperties(
                assetDirectory = assets.assetDirectory,
                serverExecutablePath = assets.serverExecutable,
                modelId = "embedded-qwen-0-5b",
                ggufFile = assets.ggufFile.fileName.toString(),
            ),
        )

        val availability = checker.check(
            model = chatModel(
                id = "embedded-qwen-0-5b",
                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                mode = ChatModelMode.EMBEDDED_OFFLINE,
                availableOffline = true,
            ),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.AVAILABLE
    }

    @Test
    fun `keeps startup availability result when files change after checker creation`() {
        val assets = runtimeAssets()
        val checker = checker(
            properties = enabledProperties(
                assetDirectory = assets.assetDirectory,
                serverExecutablePath = assets.serverExecutable,
                ggufFile = assets.ggufFile.fileName.toString(),
            ),
        )
        Files.delete(assets.ggufFile)

        val availability = checker.check(
            model = embeddedModel(),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.AVAILABLE
    }

    @Test
    fun `marks embedded offline model as misconfigured when embedded llama model is disabled`() {
        val availability = checker(properties = disabledProperties()).check(
            model = embeddedModel(),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model without offline mode as misconfigured`() {
        val availability = checker(properties = enabledProperties()).check(
            model = chatModel(
                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                mode = ChatModelMode.LOCAL_SERVER,
                availableOffline = true,
            ),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model without offline flag as misconfigured`() {
        val availability = checker(properties = enabledProperties()).check(
            model = chatModel(
                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                mode = ChatModelMode.EMBEDDED_OFFLINE,
                availableOffline = false,
            ),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model as misconfigured when model is not in embedded llama config`() {
        val availability = checker(properties = enabledProperties(modelId = "other-model")).check(
            model = embeddedModel(),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model as misconfigured when asset directory is missing`() {
        val assets = runtimeAssets()
        val missingAssetDirectory = tempDirectory.resolve("missing-assets")
        val availability = checker(
            properties = enabledProperties(
                assetDirectory = missingAssetDirectory,
                serverExecutablePath = assets.serverExecutable,
            ),
        ).check(
            model = embeddedModel(),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model as misconfigured when server executable is missing`() {
        val assets = runtimeAssets()
        val availability = checker(
            properties = enabledProperties(
                assetDirectory = assets.assetDirectory,
                serverExecutablePath = tempDirectory.resolve("missing-llama-server"),
                ggufFile = assets.ggufFile.fileName.toString(),
            ),
        ).check(
            model = embeddedModel(),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model as misconfigured when server executable is not executable`() {
        val assets = runtimeAssets()
        val nonExecutableServer = Files.createTempFile(tempDirectory, "llama-server-not-executable-", "")
        nonExecutableServer.toFile().setExecutable(false)

        val availability = checker(
            properties = enabledProperties(
                assetDirectory = assets.assetDirectory,
                serverExecutablePath = nonExecutableServer,
                ggufFile = assets.ggufFile.fileName.toString(),
            ),
        ).check(
            model = embeddedModel(),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model as misconfigured when gguf file is missing`() {
        val assets = runtimeAssets()
        val availability = checker(
            properties = enabledProperties(
                assetDirectory = assets.assetDirectory,
                serverExecutablePath = assets.serverExecutable,
                ggufFile = "missing-model.gguf",
            ),
        ).check(
            model = embeddedModel(),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model as unavailable when runtime readiness fails`() {
        val assets = runtimeAssets()
        val availability = checker(
            properties = enabledProperties(
                assetDirectory = assets.assetDirectory,
                serverExecutablePath = assets.serverExecutable,
                ggufFile = assets.ggufFile.fileName.toString(),
            ),
            runtimeReady = false,
        ).check(
            model = embeddedModel(),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.UNAVAILABLE
    }

    private fun checker(
        properties: EmbeddedLlamaProperties = disabledProperties(),
        runtimeReady: Boolean = true,
    ): EmbeddedOfflineModelAvailabilityChecker =
        EmbeddedOfflineModelAvailabilityChecker(
            embeddedLlamaProperties = properties,
            embeddedLlamaProcessManager = processManager(
                properties = properties,
                runtimeReady = runtimeReady,
            ),
        )

    private fun processManager(
        properties: EmbeddedLlamaProperties,
        runtimeReady: Boolean,
    ): EmbeddedLlamaProcessManager =
        EmbeddedLlamaProcessManager(
            chatModelRegistry = ChatModelRegistry(
                ConfiguredChatModelsProperties(
                    chatModels = listOf(
                        ConfiguredChatModelProperties(
                            id = "embedded-qwen-0-5b",
                            enabled = true,
                            config = EnabledChatModelProperties(
                                displayName = "Embedded Qwen 0.5B",
                                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                                mode = ChatModelMode.EMBEDDED_OFFLINE,
                                availableOffline = true,
                            ),
                        ),
                    ),
                ),
            ),
            embeddedLlamaProperties = properties,
            portAllocator = EphemeralEmbeddedLlamaPortAllocator { 19001 },
            processLauncher = FakeEmbeddedLlamaProcessLauncher(),
            readinessProbe = LlamaServerReadinessProbe { _, _ -> runtimeReady },
            processOutputLogger = EmbeddedLlamaProcessOutputLogger(lineConsumer = { _, _, _ -> }),
        ).also { it.run(DefaultApplicationArguments()) }

    private fun embeddedModel(): ChatModelDescriptor =
        chatModel(
            id = "embedded-qwen-0-5b",
            runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
            mode = ChatModelMode.EMBEDDED_OFFLINE,
            availableOffline = true,
        )

    private fun enabledProperties(
        assetDirectory: Path? = null,
        serverExecutablePath: Path? = null,
        modelId: String = "embedded-qwen-0-5b",
        ggufFile: String? = null,
    ): EmbeddedLlamaProperties {
        val assets = runtimeAssets()
        val configuredAssetDirectory = assetDirectory ?: assets.assetDirectory
        val configuredServerExecutablePath = serverExecutablePath ?: assets.serverExecutable
        val configuredGgufFile = ggufFile ?: assets.ggufFile.fileName.toString()

        return EmbeddedLlamaProperties(
            assetDirectory = configuredAssetDirectory.toString(),
            serverExecutablePath = configuredServerExecutablePath.toString(),
            models = listOf(
                EmbeddedLlamaModelProperties(
                    id = modelId,
                    enabled = true,
                    displayName = "Embedded Qwen 0.5B",
                    ggufFile = configuredGgufFile,
                    contextSize = 4096,
                ),
            ),
        )
    }

    private fun disabledProperties(): EmbeddedLlamaProperties {
        val assets = runtimeAssets()
        return EmbeddedLlamaProperties(
            assetDirectory = assets.assetDirectory.toString(),
            serverExecutablePath = assets.serverExecutable.toString(),
            models = listOf(
                EmbeddedLlamaModelProperties(
                    id = "embedded-qwen-0-5b",
                    enabled = false,
                    displayName = "Embedded Qwen 0.5B",
                    ggufFile = assets.ggufFile.fileName.toString(),
                    contextSize = 4096,
                ),
            ),
        )
    }

    private fun runtimeAssets(): RuntimeAssets {
        val assetDirectory = Files.createTempDirectory(tempDirectory, "llama-assets-")
        val ggufFile = Files.createFile(assetDirectory.resolve("model.gguf"))
        Files.writeString(ggufFile, "test model")
        val serverExecutable = Files.createTempFile(tempDirectory, "llama-server-", "")
        serverExecutable.toFile().setExecutable(true)
        return RuntimeAssets(
            assetDirectory = assetDirectory,
            ggufFile = ggufFile,
            serverExecutable = serverExecutable,
        )
    }

    private data class RuntimeAssets(
        val assetDirectory: Path,
        val ggufFile: Path,
        val serverExecutable: Path,
    )

    private class FakeEmbeddedLlamaProcessLauncher : EmbeddedLlamaProcessLauncher {
        override fun start(command: List<String>): Process =
            FakeProcess()
    }

    private class FakeProcess : Process() {
        private var destroyed: Boolean = false

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

        override fun exitValue(): Int =
            if (destroyed) 0 else throw IllegalThreadStateException()

        override fun destroy() {
            destroyed = true
        }

        override fun isAlive(): Boolean =
            !destroyed
    }
}
