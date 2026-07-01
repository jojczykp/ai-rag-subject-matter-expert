package org.alterbit.aisme.embedding

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import org.junit.jupiter.api.Test

class OnnxEmbeddingClientIntegrationTest {
    @Test
    fun `generates embedding with configured local ONNX model`() {
        val properties = EmbeddingModelProperties()
        val client = OnnxEmbeddingClient(
            properties = properties,
            loader = DefaultOnnxEmbeddingModelLoader(),
        )

        val embedding = client.embed("How do I boil rice?")

        embedding.model shouldBe properties.metadata
        embedding.values shouldHaveSize properties.metadata.dimensions
        embedding.values.sumOf { abs(it) } shouldBeGreaterThan 0.0

        client.close()
    }
}
