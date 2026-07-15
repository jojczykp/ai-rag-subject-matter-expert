package org.alterbit.aisme.chat.catalog

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test

class ChatModelAvailabilityCheckerTest {
    @Test
    fun `checks model availability`() {
        val checker = object : ChatModelAvailabilityChecker {
            override fun supports(model: ChatModelDescriptor): Boolean =
                model.runtime == ChatModelRuntime.OLLAMA

            override fun check(model: ChatModelDescriptor, apiTimeout: Duration): ChatModelAvailability =
                ChatModelAvailability.AVAILABLE
        }

        checker.supports(chatModel(runtime = ChatModelRuntime.OLLAMA)) shouldBe true
        checker.supports(chatModel(runtime = ChatModelRuntime.SPRING_AI)) shouldBe false
        checker.check(
            model = chatModel(runtime = ChatModelRuntime.OLLAMA),
            apiTimeout = Duration.ofSeconds(5),
        ) shouldBe ChatModelAvailability.AVAILABLE
    }
}
