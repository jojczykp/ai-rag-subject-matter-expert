package org.alterbit.aisme.chat.api

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.alterbit.aisme.chat.AiModelClientNotFoundException
import org.alterbit.aisme.chat.AiModelProviderException
import org.alterbit.aisme.chat.AiModelProviderTimeoutException
import org.alterbit.aisme.chat.catalog.ChatModelAvailability
import org.alterbit.aisme.chat.catalog.ChatModelNotFoundException
import org.alterbit.aisme.chat.catalog.ChatModelUnavailableException
import org.alterbit.aisme.web.ApiErrorCode
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ChatExceptionHandlerTest {
    private val handler = ChatExceptionHandler()

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
    fun `handles provider apiTimeout`() {
        val response = handler.handleAiModelProviderTimeout(
            AiModelProviderTimeoutException(
                modelId = "cloud-gpt",
                provider = "OpenAI-compatible",
                cause = RuntimeException("apiTimeout details"),
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
}
