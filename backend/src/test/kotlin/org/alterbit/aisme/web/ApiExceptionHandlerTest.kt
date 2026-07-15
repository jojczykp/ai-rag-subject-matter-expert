package org.alterbit.aisme.web

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
    fun `handles unexpected exception without details`() {
        val response = handler.handleUnexpected(RuntimeException("database details"))
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        body.code shouldBe ApiErrorCode.INTERNAL_ERROR
        body.message shouldBe "Unexpected server error."
        body.details shouldBe emptyMap()
    }
}
