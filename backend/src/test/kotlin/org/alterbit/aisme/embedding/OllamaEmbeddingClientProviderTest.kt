package org.alterbit.aisme.embedding

import io.kotest.matchers.collections.shouldContainExactly
import java.time.Duration
import org.junit.jupiter.api.Test

class OllamaEmbeddingClientProviderTest {
    @Test
    fun `creates one client per enabled ollama embedding model`() {
        val factory = FakeOllamaEmbeddingApiFactory()
        val provider = OllamaEmbeddingClientProvider(
            embeddingModelRegistry = EmbeddingModelRegistry(
                EmbeddingProperties(
                    runtimesById = mapOf(
                        "local-onnx" to EmbeddingRuntimeConfigProperties(type = EmbeddingModelRuntime.ONNX),
                        "local-ollama" to EmbeddingRuntimeConfigProperties(
                            type = EmbeddingModelRuntime.OLLAMA,
                            baseUrl = "http://localhost:11434",
                        ),
                    ),
                    modelsById = mapOf(
                        "local-bge-small" to onnxModel(),
                        "ollama-nomic-embed" to ollamaModel(enabled = true),
                        "disabled-ollama" to ollamaModel(enabled = false),
                    ),
                ),
            ),
            embeddingApiFactory = factory,
        )

        provider.clients().map(EmbeddingClient::modelId) shouldContainExactly listOf("ollama-nomic-embed")
        factory.createdBaseUrls shouldContainExactly listOf("http://localhost:11434")
    }

    private fun onnxModel(): EmbeddingModelConfigProperties =
        EmbeddingModelConfigProperties(
            enabled = true,
            displayName = "Local BGE Small",
            version = "1.5",
            dimensions = 384,
            runtime = EmbeddingModelRuntimeProperties(
                id = "local-onnx",
                modelPath = "./models/model.onnx",
                tokenizerPath = "./models/tokenizer.json",
            ),
        )

    private fun ollamaModel(enabled: Boolean): EmbeddingModelConfigProperties =
        EmbeddingModelConfigProperties(
            enabled = enabled,
            displayName = "Ollama Nomic Embed",
            version = "v1.5",
            dimensions = 768,
            runtime = EmbeddingModelRuntimeProperties(
                id = "local-ollama",
                modelName = "nomic-embed-text:v1.5",
            ),
        )

    private class FakeOllamaEmbeddingApiFactory : OllamaEmbeddingApiFactory {
        val createdBaseUrls = mutableListOf<String>()

        override fun create(baseUrl: String, apiTimeout: Duration): OllamaEmbeddingApi {
            createdBaseUrls += baseUrl
            return object : OllamaEmbeddingApi {
                override fun embed(request: OllamaEmbeddingRequest): OllamaEmbeddingResponse =
                    OllamaEmbeddingResponse(embeddings = listOf(listOf(1.0)))

                override fun modelNames(): Set<String> =
                    emptySet()
            }
        }
    }
}
