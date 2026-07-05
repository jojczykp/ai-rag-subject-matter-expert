package org.alterbit.aisme.chat.embedded

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class EmbeddedLlamaPropertiesTest {
    @Test
    fun `is disabled by default without nested config`() {
        val properties = EmbeddedLlamaProperties()

        properties.enabled shouldBe false
        properties.config shouldBe null
    }

    @Test
    fun `rejects enabled properties without nested config`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddedLlamaProperties(enabled = true)
        }

        exception.message shouldContain "config"
    }

    @Test
    fun `requires enabled properties before returning nested config`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddedLlamaProperties().requireEnabledConfig()
        }

        exception.message shouldContain "enabled"
    }

    @Test
    fun `returns nested config when enabled`() {
        val config = enabledConfig()
        val properties = EmbeddedLlamaProperties(enabled = true, config = config)

        properties.requireEnabledConfig() shouldBe config
    }

    @Test
    fun `rejects blank asset directory`() {
        val exception = shouldThrow<IllegalArgumentException> {
            enabledConfig(assetDirectory = " ")
        }

        exception.message shouldContain "asset-directory"
    }

    @Test
    fun `rejects blank server executable path`() {
        val exception = shouldThrow<IllegalArgumentException> {
            enabledConfig(serverExecutablePath = " ")
        }

        exception.message shouldContain "server-executable-path"
    }

    @Test
    fun `rejects blank host`() {
        val exception = shouldThrow<IllegalArgumentException> {
            enabledConfig(host = " ")
        }

        exception.message shouldContain "host"
    }

    @Test
    fun `rejects invalid port`() {
        val exception = shouldThrow<IllegalArgumentException> {
            enabledConfig(port = 0)
        }

        exception.message shouldContain "port"
    }

    @Test
    fun `rejects empty model list`() {
        val exception = shouldThrow<IllegalArgumentException> {
            enabledConfig(models = emptyList())
        }

        exception.message shouldContain "models"
    }

    @Test
    fun `rejects duplicate model ids`() {
        val exception = shouldThrow<IllegalArgumentException> {
            enabledConfig(
                models = listOf(
                    embeddedModel(id = "duplicate-model"),
                    embeddedModel(id = "duplicate-model"),
                ),
            )
        }

        exception.message shouldContain "duplicate"
    }

    @Test
    fun `rejects blank model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedModel(id = " ")
        }

        exception.message shouldContain "id"
    }

    @Test
    fun `rejects blank model display name`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedModel(displayName = " ")
        }

        exception.message shouldContain "display-name"
    }

    @Test
    fun `rejects blank gguf file`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedModel(ggufFile = " ")
        }

        exception.message shouldContain "gguf-file"
    }

    @Test
    fun `rejects non-positive context size`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedModel(contextSize = 0)
        }

        exception.message shouldContain "context-size"
    }

    @Test
    fun `rejects blank runtime argument`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedModel(runtimeArguments = listOf("--threads", " "))
        }

        exception.message shouldContain "runtime-arguments"
    }

    @Test
    fun `rejects invalid sha256`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedModel(sha256 = "not-a-sha")
        }

        exception.message shouldContain "sha256"
    }

    @Test
    fun `rejects blank license`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedModel(license = " ")
        }

        exception.message shouldContain "license"
    }

    @Test
    fun `rejects blank hardware requirements`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedModel(hardwareRequirements = " ")
        }

        exception.message shouldContain "hardware-requirements"
    }

    private fun enabledConfig(
        assetDirectory: String = "./models/llama",
        serverExecutablePath: String = "./bin/llama-server",
        host: String = "127.0.0.1",
        port: Int = 18080,
        models: List<EmbeddedLlamaModelProperties> = listOf(embeddedModel()),
    ): EnabledEmbeddedLlamaProperties =
        EnabledEmbeddedLlamaProperties(
            assetDirectory = assetDirectory,
            serverExecutablePath = serverExecutablePath,
            host = host,
            port = port,
            models = models,
        )

    private fun embeddedModel(
        id: String = "embedded-llama",
        displayName: String = "Embedded Llama",
        ggufFile: String = "models/llama.gguf",
        contextSize: Int = 4096,
        runtimeArguments: List<String> = emptyList(),
        sha256: String? = null,
        license: String = "TODO",
        hardwareRequirements: String = "TODO",
    ): EmbeddedLlamaModelProperties =
        EmbeddedLlamaModelProperties(
            id = id,
            displayName = displayName,
            ggufFile = ggufFile,
            contextSize = contextSize,
            runtimeArguments = runtimeArguments,
            sha256 = sha256,
            license = license,
            hardwareRequirements = hardwareRequirements,
        )
}
