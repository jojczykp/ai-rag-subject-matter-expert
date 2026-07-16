package org.alterbit.aisme.chat.api

import org.alterbit.aisme.chat.ChatModelClientNotFoundException
import org.alterbit.aisme.chat.ChatModelProviderException
import org.alterbit.aisme.chat.ChatModelProviderTimeoutException
import org.alterbit.aisme.chat.catalog.ChatModelNotFoundException
import org.alterbit.aisme.chat.catalog.ChatModelUnavailableException
import org.alterbit.aisme.web.ApiErrorCode
import org.alterbit.aisme.web.ApiErrorResponse
import org.alterbit.aisme.web.apiError
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ChatExceptionHandler {
    @ExceptionHandler(ChatModelNotFoundException::class)
    fun handleChatModelNotFound(exception: ChatModelNotFoundException): ResponseEntity<ApiErrorResponse> =
        apiError(
            status = HttpStatus.NOT_FOUND,
            code = ApiErrorCode.MODEL_NOT_FOUND,
            message = "Configured chat model was not found.",
            details = mapOf("modelId" to exception.modelId),
        )

    @ExceptionHandler(ChatModelUnavailableException::class)
    fun handleChatModelUnavailable(exception: ChatModelUnavailableException): ResponseEntity<ApiErrorResponse> =
        apiError(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            code = ApiErrorCode.MODEL_UNAVAILABLE,
            message = "Configured chat model is not available.",
            details = mapOf(
                "modelId" to exception.modelId,
                "availability" to exception.availability.name,
            ),
        )

    @ExceptionHandler(ChatModelClientNotFoundException::class)
    fun handleChatModelClientNotFound(exception: ChatModelClientNotFoundException): ResponseEntity<ApiErrorResponse> =
        apiError(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            code = ApiErrorCode.MODEL_CLIENT_NOT_FOUND,
            message = "AI model client was not found for the configured chat model.",
            details = mapOf("modelId" to exception.modelId),
        )

    @ExceptionHandler(ChatModelProviderTimeoutException::class)
    fun handleChatModelProviderTimeout(exception: ChatModelProviderTimeoutException): ResponseEntity<ApiErrorResponse> =
        apiError(
            status = HttpStatus.GATEWAY_TIMEOUT,
            code = ApiErrorCode.PROVIDER_TIMEOUT,
            message = "AI model provider timed out.",
            details = exception.providerDetails(),
        )

    @ExceptionHandler(ChatModelProviderException::class)
    fun handleChatModelProviderError(exception: ChatModelProviderException): ResponseEntity<ApiErrorResponse> =
        apiError(
            status = HttpStatus.BAD_GATEWAY,
            code = ApiErrorCode.PROVIDER_ERROR,
            message = "AI model provider failed.",
            details = exception.providerDetails(),
        )

    private fun ChatModelProviderException.providerDetails(): Map<String, String> =
        mapOf(
            "modelId" to modelId,
            "provider" to provider,
        )
}
