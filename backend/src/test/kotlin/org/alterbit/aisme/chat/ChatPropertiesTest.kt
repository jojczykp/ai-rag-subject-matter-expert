package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.junit.jupiter.api.Test

class ChatPropertiesTest {
    @Test
    fun `rejects zero timeout`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatProperties(timeout = Duration.ZERO)
        }

        exception.message shouldContain "aisme.chat.timeout"
    }

    @Test
    fun `rejects negative timeout`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatProperties(timeout = Duration.ofSeconds(-1))
        }

        exception.message shouldContain "aisme.chat.timeout"
    }

    @Test
    fun `rejects non-positive relevant chunk limit`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatProperties(relevantChunkLimit = 0)
        }

        exception.message shouldContain "aisme.chat.relevant-chunk-limit"
    }
}
