package org.alterbit.aisme.embedding

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class OllamaEmbeddingClientTest {
    @Test
    fun `returns embedding vector with configured model metadata`() {
        val api = FakeOllamaEmbeddingApi(
            response = OllamaEmbeddingResponse(
                embeddings = listOf(listOf(0.1, 0.2, 0.3)),
            ),
        )
        val client = OllamaEmbeddingClient(
            properties = ollamaEmbeddingProperties(),
            embeddingApi = api,
        )

        val embedding = client.embed("how to cook rice")

        embedding.values shouldBe listOf(0.1, 0.2, 0.3)
        embedding.model shouldBe EmbeddingModelMetadata(
            id = "ollama-nomic-embed",
            version = "v1.5",
            dimensions = 3,
        )
        api.requests shouldContainExactly listOf(
            OllamaEmbeddingRequest(
                model = "nomic-embed-text:v1.5",
                input = "how to cook rice",
            ),
        )
    }

    @Test
    fun `rejects blank text`() {
        val client = OllamaEmbeddingClient(
            properties = ollamaEmbeddingProperties(),
            embeddingApi = FakeOllamaEmbeddingApi(),
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.embed(" ")
        }

        exception.message shouldContain "text"
    }

    @Test
    fun `rejects embedding vector with unexpected dimensions`() {
        val client = OllamaEmbeddingClient(
            properties = ollamaEmbeddingProperties(dimensions = 3),
            embeddingApi = FakeOllamaEmbeddingApi(
                response = OllamaEmbeddingResponse(
                    embeddings = listOf(listOf(0.1, 0.2)),
                ),
            ),
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.embed("hello")
        }

        exception.message shouldContain "dimensions"
    }

    @Test
    fun `rejects response without exactly one embedding vector`() {
        val client = OllamaEmbeddingClient(
            properties = ollamaEmbeddingProperties(),
            embeddingApi = FakeOllamaEmbeddingApi(
                response = OllamaEmbeddingResponse(
                    embeddings = listOf(listOf(0.1, 0.2, 0.3), listOf(0.4, 0.5, 0.6)),
                ),
            ),
        )

        val exception = shouldThrow<EmbeddingException> {
            client.embed("hello")
        }

        exception.message shouldContain "exactly one vector"
    }

    private fun ollamaEmbeddingProperties(
        dimensions: Int = 3,
    ): EmbeddingModelProperties =
        EmbeddingModelProperties(
            id = "ollama-nomic-embed",
            version = "v1.5",
            dimensions = dimensions,
            runtime = EmbeddingModelRuntime.OLLAMA,
            baseUrl = "http://localhost:11434",
            modelName = "nomic-embed-text:v1.5",
        )

    private class FakeOllamaEmbeddingApi(
        private val response: OllamaEmbeddingResponse = OllamaEmbeddingResponse(
            embeddings = listOf(listOf(1.0, 0.0, 0.0)),
        ),
    ) : OllamaEmbeddingApi {
        val requests = mutableListOf<OllamaEmbeddingRequest>()

        override fun embed(request: OllamaEmbeddingRequest): OllamaEmbeddingResponse {
            requests += request
            return response
        }

        override fun modelNames(): Set<String> =
            emptySet()
    }
}
