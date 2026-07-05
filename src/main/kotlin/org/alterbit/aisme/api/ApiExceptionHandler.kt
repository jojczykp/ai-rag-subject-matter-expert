package org.alterbit.aisme.api

import org.alterbit.aisme.chat.AiModelClientNotFoundException
import org.alterbit.aisme.chat.AiModelProviderException
import org.alterbit.aisme.chat.AiModelProviderTimeoutException
import org.alterbit.aisme.chatmodel.ChatModelNotFoundException
import org.alterbit.aisme.chatmodel.ChatModelUnavailableException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(exception: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> =
        error(
            status = HttpStatus.BAD_REQUEST,
            code = ApiErrorCode.INVALID_REQUEST,
            message = "Request body is invalid.",
            details = exception.mostSpecificCause.message.asFieldDetail(),
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        error(
            status = HttpStatus.BAD_REQUEST,
            code = ApiErrorCode.INVALID_REQUEST,
            message = exception.message ?: "Request is invalid.",
        )

    @ExceptionHandler(ChatModelNotFoundException::class)
    fun handleChatModelNotFound(exception: ChatModelNotFoundException): ResponseEntity<ApiErrorResponse> =
        error(
            status = HttpStatus.NOT_FOUND,
            code = ApiErrorCode.MODEL_NOT_FOUND,
            message = "Configured chat model was not found.",
            details = mapOf("modelId" to exception.modelId),
        )

    @ExceptionHandler(ChatModelUnavailableException::class)
    fun handleChatModelUnavailable(exception: ChatModelUnavailableException): ResponseEntity<ApiErrorResponse> =
        error(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            code = ApiErrorCode.MODEL_UNAVAILABLE,
            message = "Configured chat model is not available.",
            details = mapOf(
                "modelId" to exception.modelId,
                "availability" to exception.availability.name,
            ),
        )

    @ExceptionHandler(AiModelClientNotFoundException::class)
    fun handleAiModelClientNotFound(exception: AiModelClientNotFoundException): ResponseEntity<ApiErrorResponse> =
        error(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            code = ApiErrorCode.MODEL_CLIENT_NOT_FOUND,
            message = "AI model client was not found for the configured chat model.",
            details = mapOf("modelId" to exception.modelId),
        )

    @ExceptionHandler(AiModelProviderTimeoutException::class)
    fun handleAiModelProviderTimeout(exception: AiModelProviderTimeoutException): ResponseEntity<ApiErrorResponse> =
        error(
            status = HttpStatus.GATEWAY_TIMEOUT,
            code = ApiErrorCode.PROVIDER_TIMEOUT,
            message = "AI model provider timed out.",
            details = exception.providerDetails(),
        )

    @ExceptionHandler(AiModelProviderException::class)
    fun handleAiModelProviderError(exception: AiModelProviderException): ResponseEntity<ApiErrorResponse> =
        error(
            status = HttpStatus.BAD_GATEWAY,
            code = ApiErrorCode.PROVIDER_ERROR,
            message = "AI model provider failed.",
            details = exception.providerDetails(),
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ApiErrorResponse> =
        error(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            code = ApiErrorCode.INTERNAL_ERROR,
            message = "Unexpected server error.",
        )

    private fun error(
        status: HttpStatus,
        code: ApiErrorCode,
        message: String,
        details: Map<String, String> = emptyMap(),
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(status)
            .body(
                ApiErrorResponse(
                    code = code,
                    message = message,
                    details = details,
                ),
            )

    private fun String?.asFieldDetail(): Map<String, String> =
        if (isNullOrBlank()) {
            emptyMap()
        } else {
            mapOf("reason" to this)
        }

    private fun AiModelProviderException.providerDetails(): Map<String, String> =
        mapOf(
            "modelId" to modelId,
            "provider" to provider,
        )
}
