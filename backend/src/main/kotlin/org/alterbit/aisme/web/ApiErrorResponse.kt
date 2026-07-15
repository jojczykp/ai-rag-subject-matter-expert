package org.alterbit.aisme.web

import com.fasterxml.jackson.annotation.JsonInclude

data class ApiErrorResponse(
    val code: ApiErrorCode,
    val message: String,
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val details: Map<String, String> = emptyMap(),
)
