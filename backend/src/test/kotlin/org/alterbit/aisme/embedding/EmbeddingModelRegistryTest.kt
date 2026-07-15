package org.alterbit.aisme.embedding

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EmbeddingModelRegistryTest {
    @Test
    fun `lists configured embedding models`() {
        val registry = EmbeddingModelRegistry(
            EmbeddingProperties(
                runtimesById = mapOf(
                    "local-onnx" to EmbeddingRuntimeConfigProperties(
                        type = EmbeddingModelRuntime.ONNX,
                    ),
                    "local-ollama" to EmbeddingRuntimeConfigProperties(
                        type = EmbeddingModelRuntime.OLLAMA,
                        baseUrl = "http://localhost:11434",
                    ),
                ),
                modelsById = mapOf(
                    "local-bge-small" to EmbeddingModelConfigProperties(
                        enabled = true,
                        displayName = "Local BGE Small",
                        version = "1.5",
                        dimensions = 384,
                        runtime = EmbeddingModelRuntimeProperties(
                            id = "local-onnx",
                            modelPath = "./models/model.onnx",
                            tokenizerPath = "./models/tokenizer.json",
                        ),
                    ),
                    "ollama-nomic-embed" to EmbeddingModelConfigProperties(
                        enabled = false,
                        displayName = "Ollama Nomic Embed",
                        version = "latest",
                        dimensions = 768,
                        runtime = EmbeddingModelRuntimeProperties(
                            id = "local-ollama",
                            modelName = "nomic-embed-text",
                        ),
                    ),
                ),
            ),
        )

        val embeddingModels = registry.embeddingModels()

        embeddingModels shouldHaveSize 2
        embeddingModels[0].id shouldBe "local-bge-small"
        embeddingModels[0].enabled shouldBe true
        embeddingModels[0].displayName shouldBe "Local BGE Small"
        embeddingModels[0].runtime shouldBe EmbeddingModelRuntime.ONNX
        embeddingModels[0].version shouldBe "1.5"
        embeddingModels[0].dimensions shouldBe 384
        embeddingModels[1].id shouldBe "ollama-nomic-embed"
        embeddingModels[1].enabled shouldBe false
        embeddingModels[1].runtime shouldBe EmbeddingModelRuntime.OLLAMA
    }
}
