package org.alterbit.aisme.embedding.runtime.ollama

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Duration
import org.alterbit.aisme.embedding.catalog.EmbeddingModelAvailability
import org.alterbit.aisme.embedding.catalog.EmbeddingModelRuntime
import org.alterbit.aisme.embedding.catalog.embeddingModel
import org.junit.jupiter.api.Test

class OllamaEmbeddingModelAvailabilityCheckerTest {
    @Test
    fun `marks ollama embedding model available when configured model exists`() {
        val factory = FakeOllamaEmbeddingApiFactory(modelNames = setOf("nomic-embed-text:v1.5"))
        val checker = OllamaEmbeddingModelAvailabilityChecker(factory)

        val availability = checker.check(
            model = embeddingModel(runtime = EmbeddingModelRuntime.OLLAMA),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe EmbeddingModelAvailability.AVAILABLE
        factory.createdBaseUrls shouldContainExactly listOf("http://localhost:11434")
        factory.createdTimeouts shouldContainExactly listOf(Duration.ofSeconds(3))
    }

    @Test
    fun `marks ollama embedding model unavailable when configured model is missing`() {
        val checker = OllamaEmbeddingModelAvailabilityChecker(
            FakeOllamaEmbeddingApiFactory(modelNames = setOf("other-model")),
        )

        val availability = checker.check(
            model = embeddingModel(runtime = EmbeddingModelRuntime.OLLAMA),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe EmbeddingModelAvailability.UNAVAILABLE
    }

    @Test
    fun `marks ollama embedding model misconfigured when model name is missing`() {
        val checker = OllamaEmbeddingModelAvailabilityChecker(FakeOllamaEmbeddingApiFactory())

        val availability = checker.check(
            model = embeddingModel(runtime = EmbeddingModelRuntime.OLLAMA).copy(modelName = null),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe EmbeddingModelAvailability.MISCONFIGURED
    }

    @Test
    fun `marks ollama embedding model unavailable when runtime check fails`() {
        val checker = OllamaEmbeddingModelAvailabilityChecker(
            FakeOllamaEmbeddingApiFactory(failure = IllegalStateException("down")),
        )

        val availability = checker.check(
            model = embeddingModel(runtime = EmbeddingModelRuntime.OLLAMA),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe EmbeddingModelAvailability.UNAVAILABLE
    }

    private class FakeOllamaEmbeddingApiFactory(
        private val modelNames: Set<String> = emptySet(),
        private val failure: RuntimeException? = null,
    ) : OllamaEmbeddingApiFactory {
        val createdBaseUrls = mutableListOf<String>()
        val createdTimeouts = mutableListOf<Duration>()

        override fun create(baseUrl: String, apiTimeout: Duration): OllamaEmbeddingApi {
            createdBaseUrls += baseUrl
            createdTimeouts += apiTimeout
            return object : OllamaEmbeddingApi {
                override fun embed(request: OllamaEmbeddingRequest): OllamaEmbeddingResponse =
                    OllamaEmbeddingResponse(embeddings = listOf(listOf(1.0)))

                override fun modelNames(): Set<String> {
                    failure?.let { throw it }
                    return modelNames
                }
            }
        }
    }
}
