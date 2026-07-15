package org.alterbit.aisme.embedding.catalog

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class EmbeddingModelMetadataTest {
    @Test
    fun `creates metadata from embedding model properties`() {
        val metadata = EmbeddingModelMetadata(
            id = "test-model",
            version = "2026-01",
            dimensions = 768,
        )
        val properties = EmbeddingModelProperties(
            id = metadata.id,
            version = metadata.version,
            dimensions = metadata.dimensions,
        )

        properties.metadata shouldBe metadata
    }

    @Test
    fun `rejects blank model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddingModelMetadata(
                id = " ",
                version = "1",
                dimensions = 384,
            )
        }

        exception.message shouldContain "id"
    }

    @Test
    fun `rejects blank model version`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddingModelMetadata(
                id = "model",
                version = " ",
                dimensions = 384,
            )
        }

        exception.message shouldContain "version"
    }

    @Test
    fun `rejects non-positive dimensions`() {
        val exception = shouldThrow<IllegalArgumentException> {
            EmbeddingModelMetadata(
                id = "model",
                version = "1",
                dimensions = 0,
            )
        }

        exception.message shouldContain "dimensions"
    }
}
