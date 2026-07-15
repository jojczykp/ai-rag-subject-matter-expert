package org.alterbit.aisme.web

import org.springframework.http.converter.HttpMessageNotReadableException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(exception: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> =
        apiError(
            status = HttpStatus.BAD_REQUEST,
            code = ApiErrorCode.INVALID_REQUEST,
            message = "Request body is invalid.",
            details = exception.mostSpecificCause.message.asFieldDetail(),
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        apiError(
            status = HttpStatus.BAD_REQUEST,
            code = ApiErrorCode.INVALID_REQUEST,
            message = exception.message ?: "Request is invalid.",
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected server error", exception)
        return apiError(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            code = ApiErrorCode.INTERNAL_ERROR,
            message = "Unexpected server error.",
        )
    }

    private fun String?.asFieldDetail(): Map<String, String> =
        if (isNullOrBlank()) {
            emptyMap()
        } else {
            mapOf("reason" to this)
        }
}
