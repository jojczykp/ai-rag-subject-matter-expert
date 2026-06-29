package org.alterbit.aisme.api

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.alterbit.aisme.chat.AiModelClientNotFoundException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ApiExceptionHandlerTest {
    private val handler = ApiExceptionHandler()

    @Test
    fun `handles missing ai model client`() {
        val response = handler.handleAiModelClientNotFound(
            AiModelClientNotFoundException("local-ollama-llama"),
        )
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
        body.code shouldBe ApiErrorCode.MODEL_CLIENT_NOT_FOUND
        body.message shouldBe "AI model client was not found for the configured chat model."
        body.details shouldContain ("modelId" to "local-ollama-llama")
    }

    @Test
    fun `handles unexpected exception without details`() {
        val response = handler.handleUnexpected(RuntimeException("database details"))
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        body.code shouldBe ApiErrorCode.INTERNAL_ERROR
        body.message shouldBe "Unexpected server error."
        body.details shouldBe emptyMap()
    }
}
