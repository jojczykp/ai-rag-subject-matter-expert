package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.junit.jupiter.api.Test

class ChatPropertiesTest {
    @Test
    fun `rejects zero apiTimeout`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatProperties(apiTimeout = Duration.ZERO)
        }

        exception.message shouldContain "aisme.chat.api-timeout"
    }

    @Test
    fun `rejects negative apiTimeout`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatProperties(apiTimeout = Duration.ofSeconds(-1))
        }

        exception.message shouldContain "aisme.chat.api-timeout"
    }

    @Test
    fun `rejects non-positive relevant chunk limit`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatProperties(retrievedChunkLimit = 0)
        }

        exception.message shouldContain "aisme.chat.retrieved-chunk-limit"
    }
}
