package org.alterbit.aisme.embedding

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class EmbeddingModelPropertiesTest {
    @Test
    fun `rejects blank model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddingModelProperties(id = " ")
        }

        exception.message shouldContain "id"
    }

    @Test
    fun `rejects blank model version`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddingModelProperties(version = " ")
        }

        exception.message shouldContain "version"
    }

    @Test
    fun `rejects blank model path`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddingModelProperties(modelPath = " ")
        }

        exception.message shouldContain "model-path"
    }

    @Test
    fun `rejects blank tokenizer path`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddingModelProperties(tokenizerPath = " ")
        }

        exception.message shouldContain "tokenizer-path"
    }

    @Test
    fun `rejects non-positive dimensions`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddingModelProperties(dimensions = 0)
        }

        exception.message shouldContain "dimensions"
    }
}
