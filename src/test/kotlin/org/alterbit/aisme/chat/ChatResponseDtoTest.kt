package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ChatResponseDtoTest {
    @Test
    fun `creates provider-neutral chat response`() {
        ChatResponseDto(
            modelId = "local-llama",
            answer = "Use a two-to-one water to rice ratio.",
        ) shouldBe ChatResponseDto(
            modelId = "local-llama",
            answer = "Use a two-to-one water to rice ratio.",
        )
    }

    @Test
    fun `rejects blank model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatResponseDto(
                modelId = " ",
                answer = "Use a two-to-one water to rice ratio.",
            )
        }

        exception.message shouldContain "modelId"
    }

    @Test
    fun `rejects blank answer`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatResponseDto(
                modelId = "local-llama",
                answer = " ",
            )
        }

        exception.message shouldContain "answer"
    }
}
