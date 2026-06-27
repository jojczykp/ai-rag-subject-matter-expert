package org.alterbit.aisme.embedding

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class OnnxEmbeddingClientTest {
    @Test
    fun `rejects blank text`() {
        val client = OnnxEmbeddingClient(
            properties = EmbeddingModelProperties(dimensions = 1),
            loader = OnnxEmbeddingModelLoader { FakeLoadedEmbeddingModel(listOf(1.0)) },
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.embed(" ")
        }

        exception.message shouldContain "text"
    }

    @Test
    fun `fails clearly when configured model file is missing`(@TempDir tempDirectory: Path) {
        val exception = shouldThrow<EmbeddingException> {
            OnnxEmbeddingClient(
                EmbeddingModelProperties(
                    modelPath = tempDirectory.resolve("missing-model.onnx").toString(),
                    tokenizerPath = tempDirectory.resolve("missing-tokenizer.json").toString(),
                ),
            )
        }

        exception.message shouldContain "model file is not readable"
    }

    @Test
    fun `returns embedding vector with configured model metadata`() {
        val model = FakeLoadedEmbeddingModel(listOf(0.6, 0.8))
        val client = OnnxEmbeddingClient(
            properties = EmbeddingModelProperties(
                id = "test-model",
                version = "test-version",
                dimensions = 2,
            ),
            loader = OnnxEmbeddingModelLoader { model },
        )

        val embedding = client.embed("hello")

        embedding shouldBe EmbeddingVector(
            values = listOf(0.6, 0.8),
            modelId = "test-model",
            modelVersion = "test-version",
            dimensions = 2,
        )
        model.embeddedTexts shouldBe listOf("hello")
    }

    @Test
    fun `loads model during construction`() {
        var loadCount = 0
        val model = FakeLoadedEmbeddingModel(listOf(1.0))
        val client = OnnxEmbeddingClient(
            properties = EmbeddingModelProperties(dimensions = 1),
            loader = OnnxEmbeddingModelLoader {
                loadCount += 1
                model
            },
        )

        loadCount shouldBe 1
        model.embeddedTexts shouldBe emptyList()

        client.embed("hello")

        model.embeddedTexts shouldBe listOf("hello")
    }

    @Test
    fun `rejects embedding vector with unexpected dimensions`() {
        val client = OnnxEmbeddingClient(
            properties = EmbeddingModelProperties(dimensions = 3),
            loader = OnnxEmbeddingModelLoader { FakeLoadedEmbeddingModel(listOf(0.1, 0.2)) },
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.embed("hello")
        }

        exception.message shouldContain "dimensions"
    }

    @Test
    fun `closes loaded model`() {
        val model = FakeLoadedEmbeddingModel(listOf(1.0))
        val client = OnnxEmbeddingClient(
            properties = EmbeddingModelProperties(dimensions = 1),
            loader = OnnxEmbeddingModelLoader { model },
        )

        client.embed("hello")
        client.close()

        model.closed shouldBe true
    }

    private class FakeLoadedEmbeddingModel(
        private val embedding: List<Double>,
    ) : LoadedEmbeddingModel {
        val embeddedTexts = mutableListOf<String>()
        var closed = false

        override fun embed(text: String): List<Double> {
            embeddedTexts += text
            return embedding
        }

        override fun close() {
            closed = true
        }
    }
}
