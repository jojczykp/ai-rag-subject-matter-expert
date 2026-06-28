package org.alterbit.aisme.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ChatRequestDtoTest {
    @Test
    fun `creates provider-neutral chat request`() {
        val request = ChatRequestDto(
            modelId = "local-llama",
            message = "How should I cook rice?",
        )

        request.modelId shouldBe "local-llama"
        request.message shouldBe "How should I cook rice?"
    }

    @Test
    fun `rejects blank model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatRequestDto(
                modelId = " ",
                message = "How should I cook rice?",
            )
        }

        exception.message shouldContain "modelId"
    }

    @Test
    fun `rejects blank message`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatRequestDto(
                modelId = "local-llama",
                message = " ",
            )
        }

        exception.message shouldContain "message"
    }
}
