package org.alterbit.aisme.chat.embedded

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class EmbeddedLlamaPropertiesTest {
    @Test
    fun `returns only enabled models`() {
        val enabledModel = embeddedModel(id = "enabled-model", enabled = true)
        val disabledModel = embeddedModel(id = "disabled-model", enabled = false)
        val properties = embeddedLlamaProperties(models = listOf(enabledModel, disabledModel))

        properties.enabledModels() shouldBe listOf(enabledModel)
    }

    @Test
    fun `rejects blank asset directory`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedLlamaProperties(assetDirectory = " ")
        }

        exception.message shouldContain "asset-directory"
    }

    @Test
    fun `rejects blank server executable path`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedLlamaProperties(serverExecutablePath = " ")
        }

        exception.message shouldContain "server-executable-path"
    }

    @Test
    fun `rejects empty model list`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedLlamaProperties(models = emptyList())
        }

        exception.message shouldContain "models"
    }

    @Test
    fun `rejects duplicate model ids`() {
        val exception = shouldThrow<IllegalArgumentException> {
            embeddedLlamaProperties(
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

    private fun embeddedLlamaProperties(
        assetDirectory: String = "./models/llama",
        serverExecutablePath: String = "./models/llama/bin/llama-server",
        models: List<EmbeddedLlamaModelProperties> = listOf(embeddedModel()),
    ): EmbeddedLlamaProperties =
        EmbeddedLlamaProperties(
            assetDirectory = assetDirectory,
            serverExecutablePath = serverExecutablePath,
            models = models,
        )

    private fun embeddedModel(
        id: String = "embedded-llama",
        enabled: Boolean = false,
        displayName: String = "Embedded Llama",
        ggufFile: String = "models/llama.gguf",
        contextSize: Int = 4096,
        runtimeArguments: List<String> = emptyList(),
        sha256: String? = null,
    ): EmbeddedLlamaModelProperties =
        EmbeddedLlamaModelProperties(
            id = id,
            enabled = enabled,
            displayName = displayName,
            ggufFile = ggufFile,
            contextSize = contextSize,
            runtimeArguments = runtimeArguments,
            sha256 = sha256,
        )
}
