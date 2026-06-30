package org.alterbit.aisme.chatmodel

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test

class EmbeddedOfflineModelAvailabilityCheckerTest {
    private val checker = EmbeddedOfflineModelAvailabilityChecker()

    @Test
    fun `supports embedded offline runtime`() {
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
        checker.supports(chatModel(runtime = ChatModelRuntime.OLLAMA)) shouldBe false
    }

    @Test
    fun `marks valid embedded offline model as available`() {
        val availability = checker.check(
            model = chatModel(
                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                mode = ChatModelMode.EMBEDDED_OFFLINE,
                availableOffline = true,
            ),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.AVAILABLE
    }

    @Test
    fun `marks embedded offline model without offline mode as misconfigured`() {
        val availability = checker.check(
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
        val availability = checker.check(
            model = chatModel(
                runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
                mode = ChatModelMode.EMBEDDED_OFFLINE,
                availableOffline = false,
            ),
            timeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }
}
