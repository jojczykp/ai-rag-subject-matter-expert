package org.alterbit.aisme.chatmodel

import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.alterbit.aisme.chat.embedded.EnabledLlamaRuntimeProperties
import org.alterbit.aisme.chat.embedded.LlamaRuntimeModelProperties
import org.alterbit.aisme.chat.embedded.LlamaRuntimeProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

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
                modelId = "llama-runtime-example",
                ggufFile = assets.ggufFile.fileName.toString(),
            ),
        )

        val availability = checker.check(
            model = chatModel(
                id = "llama-runtime-example",
                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                mode = ChatModelMode.EMBEDDED_OFFLINE,
                availableOffline = true,
            ),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.AVAILABLE
    }

    @Test
    fun `marks embedded offline model as misconfigured when llama runtime is disabled`() {
        val availability = checker().check(
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
    fun `marks embedded offline model as misconfigured when model is not in llama runtime config`() {
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

    private fun checker(
        properties: LlamaRuntimeProperties = LlamaRuntimeProperties(),
    ): EmbeddedOfflineModelAvailabilityChecker =
        EmbeddedOfflineModelAvailabilityChecker(properties)

    private fun embeddedModel(): ChatModelDescriptor =
        chatModel(
            id = "llama-runtime-example",
            runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
            mode = ChatModelMode.EMBEDDED_OFFLINE,
            availableOffline = true,
        )

    private fun enabledProperties(
        assetDirectory: Path? = null,
        serverExecutablePath: Path? = null,
        modelId: String = "llama-runtime-example",
        ggufFile: String? = null,
    ): LlamaRuntimeProperties {
        val assets = runtimeAssets()
        val configuredAssetDirectory = assetDirectory ?: assets.assetDirectory
        val configuredServerExecutablePath = serverExecutablePath ?: assets.serverExecutable
        val configuredGgufFile = ggufFile ?: assets.ggufFile.fileName.toString()

        return LlamaRuntimeProperties(
            enabled = true,
            config = EnabledLlamaRuntimeProperties(
                assetDirectory = configuredAssetDirectory.toString(),
                serverExecutablePath = configuredServerExecutablePath.toString(),
                host = "127.0.0.1",
                port = 18080,
                models = listOf(
                    LlamaRuntimeModelProperties(
                        id = modelId,
                        displayName = "Llama Runtime Example",
                        ggufFile = configuredGgufFile,
                        contextSize = 4096,
                        license = "TODO",
                        hardwareRequirements = "TODO",
                    ),
                ),
            ),
        )
    }

    private fun runtimeAssets(): RuntimeAssets {
        val assetDirectory = Files.createTempDirectory(tempDirectory, "llama-assets-")
        val ggufFile = Files.createFile(assetDirectory.resolve("model.gguf"))
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
}
