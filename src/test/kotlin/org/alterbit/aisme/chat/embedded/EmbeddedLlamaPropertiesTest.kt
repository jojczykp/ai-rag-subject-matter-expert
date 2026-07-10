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

    private fun enabledConfig(
        assetDirectory: String = "./models/llama",
        serverExecutablePath: String = "./models/llama/bin/llama-server",
        models: List<EmbeddedLlamaModelProperties> = listOf(embeddedModel()),
    ): EnabledEmbeddedLlamaProperties =
        EnabledEmbeddedLlamaProperties(
            assetDirectory = assetDirectory,
            serverExecutablePath = serverExecutablePath,
            models = models,
        )

    private fun embeddedModel(
        id: String = "embedded-llama",
        displayName: String = "Embedded Llama",
        ggufFile: String = "models/llama.gguf",
        contextSize: Int = 4096,
        runtimeArguments: List<String> = emptyList(),
        sha256: String? = null,
    ): EmbeddedLlamaModelProperties =
        EmbeddedLlamaModelProperties(
            id = id,
            displayName = displayName,
            ggufFile = ggufFile,
            contextSize = contextSize,
            runtimeArguments = runtimeArguments,
            sha256 = sha256,
        )
}
