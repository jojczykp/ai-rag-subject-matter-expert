package org.alterbit.aisme.embedding

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Name

@ConfigurationProperties(prefix = "aisme")
data class EmbeddingModelsProperties(
    @param:Name("embedding-runtimes")
    val embeddingRuntimesById: Map<String, EmbeddingRuntimeConfigProperties> = mapOf(
        "local-onnx" to EmbeddingRuntimeConfigProperties(
            type = EmbeddingModelRuntime.ONNX,
            modelPath = "./models/bge-small-en-v1.5/model.onnx",
            tokenizerPath = "./models/bge-small-en-v1.5/tokenizer.json",
        ),
    ),
    @param:Name("embedding-models")
    val embeddingModelsById: Map<String, EmbeddingModelConfigProperties> = mapOf(
        "local-bge-small" to EmbeddingModelConfigProperties(
            enabled = true,
            version = "1.5",
            dimensions = 384,
            runtime = EmbeddingModelRuntimeProperties(id = "local-onnx"),
        ),
    ),
) {
    init {
        require(embeddingRuntimesById.isNotEmpty()) {
            "aisme.embedding-runtimes must contain at least one runtime"
        }
        require(embeddingRuntimesById.keys.none { it.isBlank() }) {
            "aisme.embedding-runtimes must not contain blank ids"
        }
        require(embeddingModelsById.isNotEmpty()) {
            "aisme.embedding-models must contain at least one model"
        }
        require(embeddingModelsById.keys.none { it.isBlank() }) {
            "aisme.embedding-models must not contain blank ids"
        }
    }

    fun activeModel(): EmbeddingModelProperties {
        val enabledModels = embeddingModelsById
            .filterValues(EmbeddingModelConfigProperties::enabled)
        require(enabledModels.size == 1) {
            "aisme.embedding-models must contain exactly one enabled model"
        }

        val (modelId, model) = enabledModels.entries.single()
        val runtimeId = requireNotNull(model.runtime.id) {
            "aisme.embedding-models.$modelId.runtime.id is required when aisme.embedding-models.$modelId.enabled is true"
        }
        val runtime = embeddingRuntimesById[runtimeId]
        require(runtime != null) {
            "aisme.embedding-models.$modelId.runtime.id references unknown runtime '$runtimeId'"
        }

        fun requireModelConfigured(value: String?, propertyName: String): String =
            requireNotNull(value) {
                "aisme.embedding-models.$modelId.$propertyName is required for ${runtime.type} embedding models"
            }

        fun requireRuntimeConfigured(value: String?, propertyName: String): String =
            requireNotNull(value) {
                "aisme.embedding-runtimes.$runtimeId.$propertyName is required for ${runtime.type} embedding runtimes"
            }

        return EmbeddingModelProperties(
            id = modelId,
            version = requireModelConfigured(model.version, "version"),
            dimensions = requireNotNull(model.dimensions) {
                "aisme.embedding-models.$modelId.dimensions is required for ${runtime.type} embedding models"
            },
            runtime = runtime.type,
            modelPath = requireRuntimeConfigured(runtime.modelPath, "model-path"),
            tokenizerPath = requireRuntimeConfigured(runtime.tokenizerPath, "tokenizer-path"),
        )
    }
}
