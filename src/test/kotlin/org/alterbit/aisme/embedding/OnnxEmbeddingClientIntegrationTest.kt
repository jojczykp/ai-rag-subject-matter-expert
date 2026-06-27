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
        val client = OnnxEmbeddingClient(properties)

        val embedding = client.embed("How do I boil rice?")

        embedding.modelId shouldBe properties.id
        embedding.modelVersion shouldBe properties.version
        embedding.dimensions shouldBe properties.dimensions
        embedding.values shouldHaveSize properties.dimensions
        embedding.values.sumOf { abs(it) } shouldBeGreaterThan 0.0

        client.close()
    }
}
