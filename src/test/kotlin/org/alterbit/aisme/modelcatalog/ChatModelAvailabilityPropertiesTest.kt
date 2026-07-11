package org.alterbit.aisme.modelcatalog

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

        exception.message shouldContain "aisme.chat-model-availability.timeout"
    }

    @Test
    fun `rejects negative timeout`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelAvailabilityProperties(timeout = Duration.ofSeconds(-1))
        }

        exception.message shouldContain "aisme.chat-model-availability.timeout"
    }

    @Test
    fun `rejects zero cache ttl`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelAvailabilityProperties(cacheTtl = Duration.ZERO)
        }

        exception.message shouldContain "aisme.chat-model-availability.cache-ttl"
    }

    @Test
    fun `rejects negative cache ttl`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelAvailabilityProperties(cacheTtl = Duration.ofSeconds(-1))
        }

        exception.message shouldContain "aisme.chat-model-availability.cache-ttl"
    }
}
