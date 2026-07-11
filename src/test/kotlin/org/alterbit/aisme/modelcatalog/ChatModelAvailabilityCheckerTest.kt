package org.alterbit.aisme.modelcatalog

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test

class ChatModelAvailabilityCheckerTest {
    @Test
    fun `checks model availability`() {
        val checker = object : ChatModelAvailabilityChecker {
            override fun supports(model: ChatModelDescriptor): Boolean =
                model.runtime == ChatModelRuntime.OLLAMA

            override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
                ChatModelAvailability.AVAILABLE
        }

        checker.supports(chatModel(runtime = ChatModelRuntime.OLLAMA)) shouldBe true
        checker.supports(chatModel(runtime = ChatModelRuntime.SPRING_AI)) shouldBe false
        checker.check(
            model = chatModel(runtime = ChatModelRuntime.OLLAMA),
            timeout = Duration.ofSeconds(5),
        ) shouldBe ChatModelAvailability.AVAILABLE
    }
}
