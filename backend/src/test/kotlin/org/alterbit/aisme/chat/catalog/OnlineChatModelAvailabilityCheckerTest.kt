package org.alterbit.aisme.chat.catalog

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test

class OnlineChatModelAvailabilityCheckerTest {
    private val checker = OnlineChatModelAvailabilityChecker()

    @Test
    fun `supports online model runtimes`() {
        checker.supports(chatModel(runtime = ChatModelRuntime.OPENAI_COMPATIBLE)) shouldBe true
        checker.supports(chatModel(runtime = ChatModelRuntime.HUGGING_FACE_TGI)) shouldBe true
        checker.supports(chatModel(runtime = ChatModelRuntime.SPRING_AI)) shouldBe true
    }

    @Test
    fun `does not support local model runtimes`() {
        checker.supports(chatModel(runtime = ChatModelRuntime.OLLAMA)) shouldBe false
        checker.supports(chatModel(runtime = ChatModelRuntime.EMBEDDED_LLAMA)) shouldBe false
    }

    @Test
    fun `marks OpenAI-compatible model without api key as misconfigured`() {
        val availability = checker.check(
            model = chatModel(
                runtime = ChatModelRuntime.OPENAI_COMPATIBLE,
                apiKey = null,
            ),
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks OpenAI-compatible model with api key as configured`() {
        val availability = checker.check(
            model = chatModel(
                runtime = ChatModelRuntime.OPENAI_COMPATIBLE,
                apiKey = "test-api-key",
            ),
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.CONFIGURED
    }

    @Test
    fun `marks Hugging Face endpoint model as configured without api key`() {
        val availability = checker.check(
            model = chatModel(
                runtime = ChatModelRuntime.HUGGING_FACE_TGI,
                apiKey = null,
            ),
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe ChatModelAvailability.CONFIGURED
    }
}
