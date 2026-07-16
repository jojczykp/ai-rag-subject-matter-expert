package org.alterbit.aisme.chat.api

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ChatRequestDtoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `creates provider-neutral chat request`() {
        val request = ChatRequestDto(
            subjectId = "culinary-expert",
            modelId = "local-llama",
            message = "How should I cook rice?",
        )

        request.subjectId shouldBe "culinary-expert"
        request.modelId shouldBe "local-llama"
        request.embeddingModelId shouldBe null
        request.message shouldBe "How should I cook rice?"
    }

    @Test
    fun `creates chat request with selected embedding model`() {
        val request = ChatRequestDto(
            subjectId = "culinary-expert",
            modelId = "local-llama",
            embeddingModelId = "local-bge-small",
            message = "How should I cook rice?",
        )

        request.modelId shouldBe "local-llama"
        request.embeddingModelId shouldBe "local-bge-small"
        request.message shouldBe "How should I cook rice?"
    }

    @Test
    fun `rejects blank subject id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatRequestDto(
                subjectId = " ",
                modelId = "local-llama",
                message = "How should I cook rice?",
            )
        }

        exception.message shouldContain "subjectId"
    }

    @Test
    fun `rejects blank model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatRequestDto(
                subjectId = "culinary-expert",
                modelId = " ",
                message = "How should I cook rice?",
            )
        }

        exception.message shouldContain "modelId"
    }

    @Test
    fun `rejects blank embedding model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatRequestDto(
                subjectId = "culinary-expert",
                modelId = "local-llama",
                embeddingModelId = " ",
                message = "How should I cook rice?",
            )
        }

        exception.message shouldContain "embeddingModelId"
    }

    @Test
    fun `rejects missing model id when deserialized from json`() {
        val exception = shouldThrow<JsonMappingException> {
            objectMapper.readValue<ChatRequestDto>(
                """
                {
                  "subjectId": "culinary-expert",
                  "message": "How should I cook rice?"
                }
                """.trimIndent(),
            )
        }

        exception.message shouldContain "modelId"
    }

    @Test
    fun `rejects blank message`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatRequestDto(
                subjectId = "culinary-expert",
                modelId = "local-llama",
                message = " ",
            )
        }

        exception.message shouldContain "message"
    }

    @Test
    fun `rejects missing message when deserialized from json`() {
        val exception = shouldThrow<JsonMappingException> {
            objectMapper.readValue<ChatRequestDto>(
                """
                {
                  "subjectId": "culinary-expert",
                  "modelId": "local-llama"
                }
                """.trimIndent(),
            )
        }

        exception.message shouldContain "message"
    }
}
