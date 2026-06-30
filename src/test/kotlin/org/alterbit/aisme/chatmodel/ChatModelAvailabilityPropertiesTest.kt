package org.alterbit.aisme.chatmodel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.junit.jupiter.api.Test

class ChatModelAvailabilityPropertiesTest {
    @Test
    fun `rejects zero timeout`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelAvailabilityProperties(timeout = Duration.ZERO)
        }

        exception.message shouldContain "aisme.model-availability.timeout"
    }

    @Test
    fun `rejects negative timeout`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelAvailabilityProperties(timeout = Duration.ofSeconds(-1))
        }

        exception.message shouldContain "aisme.model-availability.timeout"
    }
}
