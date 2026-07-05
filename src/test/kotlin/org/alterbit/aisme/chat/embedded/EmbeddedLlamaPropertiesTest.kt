package org.alterbit.aisme.chat.embedded

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class EmbeddedLlamaPropertiesTest {
    @Test
    fun `rejects blank asset directory`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddedLlamaProperties(assetDirectory = " ")
        }

        exception.message shouldContain "asset-directory"
    }

    @Test
    fun `rejects blank server executable path`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddedLlamaProperties(serverExecutablePath = " ")
        }

        exception.message shouldContain "server-executable-path"
    }

    @Test
    fun `rejects blank host`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddedLlamaProperties(host = " ")
        }

        exception.message shouldContain "host"
    }

    @Test
    fun `rejects invalid port`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddedLlamaProperties(port = 0)
        }

        exception.message shouldContain "port"
    }

    @Test
    fun `rejects duplicate model ids`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddedLlamaProperties(
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
