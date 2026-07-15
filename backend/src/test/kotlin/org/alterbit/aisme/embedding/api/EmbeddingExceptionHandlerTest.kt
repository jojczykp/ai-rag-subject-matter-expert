package org.alterbit.aisme.embedding.api

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.alterbit.aisme.embedding.catalog.EmbeddingModelNotFoundException
import org.alterbit.aisme.web.ApiErrorCode
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class EmbeddingExceptionHandlerTest {
    private val handler = EmbeddingExceptionHandler()

    @Test
    fun `handles embedding model not found`() {
        val response = handler.handleEmbeddingModelNotFound(EmbeddingModelNotFoundException("unknown-embedding"))
        val body = response.body.shouldNotBeNull()

        response.statusCode shouldBe HttpStatus.NOT_FOUND
        body.code shouldBe ApiErrorCode.MODEL_NOT_FOUND
        body.message shouldBe "Configured embedding model was not found."
        body.details shouldContain ("embeddingModelId" to "unknown-embedding")
    }
}
