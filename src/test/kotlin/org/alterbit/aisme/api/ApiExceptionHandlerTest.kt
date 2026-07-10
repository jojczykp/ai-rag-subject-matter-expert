package org.alterbit.aisme.api

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.alterbit.aisme.chat.AiModelClientNotFoundException
import org.alterbit.aisme.chat.AiModelProviderException
import org.alterbit.aisme.chat.AiModelProviderTimeoutException
import org.alterbit.aisme.chatmodel.ChatModelAvailability
import org.alterbit.aisme.chatmodel.ChatModelNotFoundException
import org.alterbit.aisme.chatmodel.ChatModelUnavailableException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage

class ApiExceptionHandlerTest {
    private val handler = ApiExceptionHandler()

    @Test
    fun `handles unreadable message`() {
        val response = handler.handleUnreadableMessage(
            HttpMessageNotReadableException(
                "JSON parse error",
                IllegalArgumentException("missing modelId"),
                MockHttpInputMessage(ByteArray(0)),
            ),
        )
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        body.code shouldBe ApiErrorCode.INVALID_REQUEST
        body.message shouldBe "Request body is invalid."
        body.details shouldContain ("reason" to "missing modelId")
    }

    @Test
    fun `handles illegal argument`() {
        val response = handler.handleIllegalArgument(IllegalArgumentException("message must not be blank"))
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        body.code shouldBe ApiErrorCode.INVALID_REQUEST
        body.message shouldBe "message must not be blank"
        body.details shouldBe emptyMap()
    }

    @Test
    fun `handles chat model not found`() {
        val response = handler.handleChatModelNotFound(ChatModelNotFoundException("unknown-model"))
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.NOT_FOUND
        body.code shouldBe ApiErrorCode.MODEL_NOT_FOUND
        body.message shouldBe "Configured chat model was not found."
        body.details shouldContain ("modelId" to "unknown-model")
    }

    @Test
    fun `handles chat model unavailable`() {
        val response = handler.handleChatModelUnavailable(
            ChatModelUnavailableException(
                modelId = "cloud-gpt",
                availability = ChatModelAvailability.UNAVAILABLE,
            ),
        )
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
        body.code shouldBe ApiErrorCode.MODEL_UNAVAILABLE
        body.message shouldBe "Configured chat model is not available."
        body.details shouldContain ("modelId" to "cloud-gpt")
        body.details shouldContain ("availability" to "UNAVAILABLE")
    }

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
    fun `handles provider timeout`() {
        val response = handler.handleAiModelProviderTimeout(
            AiModelProviderTimeoutException(
                modelId = "cloud-gpt",
                provider = "OpenAI-compatible",
                cause = RuntimeException("timeout details"),
            ),
        )
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.GATEWAY_TIMEOUT
        body.code shouldBe ApiErrorCode.PROVIDER_TIMEOUT
        body.message shouldBe "AI model provider timed out."
        body.details shouldContain ("modelId" to "cloud-gpt")
        body.details shouldContain ("provider" to "OpenAI-compatible")
    }

    @Test
    fun `handles provider error`() {
        val response = handler.handleAiModelProviderError(
            AiModelProviderException(
                modelId = "cloud-gpt",
                provider = "OpenAI-compatible",
                message = "provider details",
            ),
        )
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.BAD_GATEWAY
        body.code shouldBe ApiErrorCode.PROVIDER_ERROR
        body.message shouldBe "AI model provider failed."
        body.details shouldContain ("modelId" to "cloud-gpt")
        body.details shouldContain ("provider" to "OpenAI-compatible")
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
