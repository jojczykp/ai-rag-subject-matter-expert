package org.alterbit.aisme.embedding

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Name

@ConfigurationProperties(prefix = "aisme.embedding")
data class EmbeddingProperties(
    @param:Name("runtimes")
    val runtimesById: Map<String, EmbeddingRuntimeConfigProperties> = mapOf(
        "local-onnx" to EmbeddingRuntimeConfigProperties(
            type = EmbeddingModelRuntime.ONNX,
        ),
        "local-ollama" to EmbeddingRuntimeConfigProperties(
            type = EmbeddingModelRuntime.OLLAMA,
            baseUrl = "http://localhost:11434",
        ),
    ),
    @param:Name("models")
    val modelsById: Map<String, EmbeddingModelConfigProperties> = mapOf(
        "local-bge-small" to EmbeddingModelConfigProperties(
            enabled = true,
            displayName = "Local BGE Small",
            version = "1.5",
            dimensions = 384,
            runtime = EmbeddingModelRuntimeProperties(
                id = "local-onnx",
                modelPath = "./models/bge-small-en-v1.5/model.onnx",
                tokenizerPath = "./models/bge-small-en-v1.5/tokenizer.json",
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
) {
    init {
        require(runtimesById.isNotEmpty()) {
            "aisme.embedding.runtimes must contain at least one runtime"
        }
        require(runtimesById.keys.none { it.isBlank() }) {
            "aisme.embedding.runtimes must not contain blank ids"
        }
        require(modelsById.isNotEmpty()) {
            "aisme.embedding.models must contain at least one model"
        }
        require(modelsById.keys.none { it.isBlank() }) {
            "aisme.embedding.models must not contain blank ids"
        }
    }

    fun activeModel(): EmbeddingModelProperties {
        val enabledModels = modelsById
            .filterValues(EmbeddingModelConfigProperties::enabled)
        require(enabledModels.size == 1) {
            "aisme.embedding.models must contain exactly one enabled model"
        }

        val (modelId, model) = enabledModels.entries.single()
        val runtimeId = requireNotNull(model.runtime.id) {
            "aisme.embedding.models.$modelId.runtime.id is required when aisme.embedding.models.$modelId.enabled is true"
        }
        val runtime = runtimesById[runtimeId]
        require(runtime != null) {
            "aisme.embedding.models.$modelId.runtime.id references unknown runtime '$runtimeId'"
        }

        fun requireModelConfigured(value: String?, propertyName: String): String =
            requireNotNull(value) {
                "aisme.embedding.models.$modelId.$propertyName is required for ${runtime.type} embedding models"
            }

        val version = requireModelConfigured(model.version, "version")
        val dimensions = requireNotNull(model.dimensions) {
            "aisme.embedding.models.$modelId.dimensions is required for ${runtime.type} embedding models"
        }

        return when (runtime.type) {
            EmbeddingModelRuntime.ONNX -> EmbeddingModelProperties(
                id = modelId,
                version = version,
                dimensions = dimensions,
                runtime = runtime.type,
                modelPath = requireModelConfigured(model.runtime.modelPath, "runtime.model-path"),
                tokenizerPath = requireModelConfigured(model.runtime.tokenizerPath, "runtime.tokenizer-path"),
            )

            EmbeddingModelRuntime.OLLAMA -> error("OLLAMA embedding models are configurable but not supported for indexing yet")
        }
    }
}
