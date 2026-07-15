package org.alterbit.aisme.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

fun apiError(
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
