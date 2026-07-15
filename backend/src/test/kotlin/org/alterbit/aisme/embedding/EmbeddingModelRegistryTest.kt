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
                        displayOrder = 20,
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
                        displayOrder = 10,
                        displayName = "Ollama Nomic Embed",
                        version = "v1.5",
                        dimensions = 768,
                        runtime = EmbeddingModelRuntimeProperties(
                            id = "local-ollama",
                            modelName = "nomic-embed-text:v1.5",
                        ),
                    ),
                ),
            ),
        )

        val embeddingModels = registry.embeddingModels()

        embeddingModels shouldHaveSize 2
        embeddingModels[0].id shouldBe "ollama-nomic-embed"
        embeddingModels[0].enabled shouldBe false
        embeddingModels[0].displayOrder shouldBe 10
        embeddingModels[0].runtime shouldBe EmbeddingModelRuntime.OLLAMA
        embeddingModels[0].mode shouldBe EmbeddingModelMode.LOCAL_SERVER
        embeddingModels[1].id shouldBe "local-bge-small"
        embeddingModels[1].enabled shouldBe true
        embeddingModels[1].displayOrder shouldBe 20
        embeddingModels[1].displayName shouldBe "Local BGE Small"
        embeddingModels[1].runtime shouldBe EmbeddingModelRuntime.ONNX
        embeddingModels[1].mode shouldBe EmbeddingModelMode.EMBEDDED_OFFLINE
        embeddingModels[1].version shouldBe "1.5"
        embeddingModels[1].dimensions shouldBe 384
    }
}
