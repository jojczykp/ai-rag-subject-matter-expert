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
}
