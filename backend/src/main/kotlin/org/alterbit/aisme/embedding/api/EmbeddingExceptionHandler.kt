package org.alterbit.aisme.embedding.api

import org.alterbit.aisme.embedding.catalog.EmbeddingModelNotFoundException
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
class EmbeddingExceptionHandler {
    @ExceptionHandler(EmbeddingModelNotFoundException::class)
    fun handleEmbeddingModelNotFound(exception: EmbeddingModelNotFoundException): ResponseEntity<ApiErrorResponse> =
        apiError(
            status = HttpStatus.NOT_FOUND,
            code = ApiErrorCode.MODEL_NOT_FOUND,
            message = "Configured embedding model was not found.",
            details = mapOf("embeddingModelId" to exception.modelId),
        )

}
