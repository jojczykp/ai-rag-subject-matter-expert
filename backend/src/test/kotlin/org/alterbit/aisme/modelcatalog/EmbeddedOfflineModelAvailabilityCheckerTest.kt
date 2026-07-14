package org.alterbit.aisme.modelcatalog

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.alterbit.aisme.chat.embedded.EphemeralEmbeddedLlamaPortAllocator
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProcessLauncher
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProcessManager
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProcessOutputLogger
import org.alterbit.aisme.chat.embedded.LlamaServerReadinessProbe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
class EmbeddedOfflineModelAvailabilityCheckerTest {
    @field:TempDir
    lateinit var tempDirectory: Path

    @Test
    fun `supports embedded offline runtime`() {
        val checker = checker()

        checker.supports(
            chatModel(
                runtime = ChatModelRuntime.EMBEDDED_LLAMA,
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
        val model = embeddedModel(assets = assets)
        val checker = checker(model = model)

        val availability = checker.check(
            model = model,
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.AVAILABLE
    }

    @Test
    fun `detects static asset changes after checker creation`() {
        val assets = runtimeAssets()
        val model = embeddedModel(assets = assets)
        val checker = checker(model = model)
        Files.delete(assets.ggufFile)

        val availability = checker.check(
            model = model,
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model without offline mode as misconfigured`() {
        val availability = checker().check(
            model = chatModel(
                runtime = ChatModelRuntime.EMBEDDED_LLAMA,
                mode = ChatModelMode.LOCAL_SERVER,
                availableOffline = true,
            ),
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model without offline flag as misconfigured`() {
        val availability = checker().check(
            model = chatModel(
                runtime = ChatModelRuntime.EMBEDDED_LLAMA,
                mode = ChatModelMode.EMBEDDED_OFFLINE,
                availableOffline = false,
            ),
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model as misconfigured when asset directory is missing`() {
        val assets = runtimeAssets()
        val missingAssetDirectory = tempDirectory.resolve("missing-assets")
        val model = embeddedModel(
            assets = assets,
            assetDirectory = missingAssetDirectory,
        )
        val availability = checker(model = model).check(
            model = model,
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model as misconfigured when server executable is missing`() {
        val assets = runtimeAssets()
        val model = embeddedModel(
            assets = assets,
            serverExecutablePath = tempDirectory.resolve("missing-llama-server"),
        )
        val availability = checker(model = model).check(
            model = model,
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks embedded offline model as misconfigured when server executable is not executable`() {
        val assets = runtimeAssets()
        val nonExecutableServer = Files.createTempFile(tempDirectory, "llama-server-not-executable-", "")
        nonExecutableServer.toFile().setExecutable(false)
        val model = embeddedModel(
            assets = assets,
            serverExecutablePath = nonExecutableServer,
        )

        val availability = checker(model = model).check(
            model = model,
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `logs clear warning when gguf file is missing`(output: CapturedOutput) {
        val assets = runtimeAssets()
        val model = embeddedModel(
            assets = assets,
            ggufFile = "missing-model.gguf",
        )
        val availability = checker(model = model).check(
            model = model,
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
        output.toString() shouldContain
            "Embedded model 'embedded-qwen-0-5b' GGUF model file not found: ${assets.assetDirectory.resolve("missing-model.gguf")}"
    }

    @Test
    fun `marks embedded offline model as unavailable when runtime readiness fails`() {
        val assets = runtimeAssets()
        val model = embeddedModel(assets = assets)
        val availability = checker(
            model = model,
            runtimeReady = false,
        ).check(
            model = model,
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.UNAVAILABLE
    }

    private fun checker(
        model: ChatModelDescriptor = embeddedModel(),
        runtimeReady: Boolean = true,
    ): EmbeddedOfflineModelAvailabilityChecker =
        EmbeddedOfflineModelAvailabilityChecker(
            embeddedLlamaProcessManager = processManager(
                model = model,
                runtimeReady = runtimeReady,
            ),
        )

    private fun processManager(
        model: ChatModelDescriptor,
        runtimeReady: Boolean,
    ): EmbeddedLlamaProcessManager =
        EmbeddedLlamaProcessManager(
            chatModelRegistry = ChatModelRegistry(
                ChatModelsProperties(
                    chatRuntimesById = mapOf(
                        "embedded-llama" to ChatModelRuntimeConfigProperties(
                            type = ChatModelRuntime.EMBEDDED_LLAMA,
                            assetDirectory = model.assetDirectory,
                            serverExecutablePath = model.serverExecutablePath,
                        ),
                    ),
                    chatModelsById = mapOf(
                        model.id to ChatModelProperties(
                            enabled = true,
                            displayName = model.displayName,
                            runtime = ChatModelRuntimeProperties(
                                id = model.runtimeId,
                                modelName = model.modelName,
                                ggufFile = model.ggufFile,
                                contextSize = model.contextSize,
                                runtimeArguments = model.runtimeArguments,
                            ),
                        ),
                    ),
                ),
            ),
            portAllocator = EphemeralEmbeddedLlamaPortAllocator { 19001 },
            processLauncher = FakeEmbeddedLlamaProcessLauncher(),
            readinessProbe = LlamaServerReadinessProbe { _, _ -> runtimeReady },
            processOutputLogger = EmbeddedLlamaProcessOutputLogger(lineConsumer = { _, _, _ -> }),
        ).also { it.run(DefaultApplicationArguments()) }

    private fun embeddedModel(
        assets: RuntimeAssets = runtimeAssets(),
        assetDirectory: Path = assets.assetDirectory,
        serverExecutablePath: Path = assets.serverExecutable,
        ggufFile: String = assets.ggufFile.fileName.toString(),
    ): ChatModelDescriptor =
        chatModel(
            id = "embedded-qwen-0-5b",
            runtimeId = "embedded-llama",
            runtime = ChatModelRuntime.EMBEDDED_LLAMA,
            mode = ChatModelMode.EMBEDDED_OFFLINE,
            availableOffline = true,
            assetDirectory = assetDirectory.toString(),
            serverExecutablePath = serverExecutablePath.toString(),
            modelName = "qwen2.5",
            ggufFile = ggufFile,
            contextSize = 4096,
        )

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
