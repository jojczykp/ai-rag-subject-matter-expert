package org.alterbit.aisme.document.api

import org.alterbit.aisme.document.SubjectNotFoundException
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
class SubjectExceptionHandler {
    @ExceptionHandler(SubjectNotFoundException::class)
    fun handleSubjectNotFound(exception: SubjectNotFoundException): ResponseEntity<ApiErrorResponse> =
        apiError(
            status = HttpStatus.NOT_FOUND,
            code = ApiErrorCode.SUBJECT_NOT_FOUND,
            message = "Configured subject was not found.",
            details = mapOf("subjectId" to exception.subjectId),
        )
}
