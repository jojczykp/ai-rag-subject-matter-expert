package org.alterbit.aisme.embedding.catalog

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Name

@ConfigurationProperties(prefix = "aisme.embedding")
data class EmbeddingProperties(
    val apiTimeout: Duration = Duration.ofSeconds(60),
    @param:Name("default-model-id")
    val defaultModelId: String? = null,
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
            displayOrder = 10,
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
            enabled = true,
            displayOrder = 20,
            displayName = "Ollama Nomic Embed",
            version = "v1.5",
            dimensions = 768,
            runtime = EmbeddingModelRuntimeProperties(
                id = "local-ollama",
                modelName = "nomic-embed-text:v1.5",
            ),
        ),
    ),
) {
    init {
        require(apiTimeout.isPositive) {
            "aisme.embedding.api-timeout must be greater than zero"
        }
        require(defaultModelId == null || defaultModelId.isNotBlank()) {
            "aisme.embedding.default-model-id must not be blank when configured"
        }
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
        require(defaultModelId == null || modelsById[defaultModelId]?.enabled == true) {
            "aisme.embedding.default-model-id references unknown or disabled model '$defaultModelId'"
        }
    }

    fun activeModel(): EmbeddingModelProperties {
        val enabledModels = modelsById
            .filterValues(EmbeddingModelConfigProperties::enabled)
        require(enabledModels.size == 1) {
            "aisme.embedding.models must contain exactly one enabled model"
        }

        val (modelId, model) = enabledModels.entries.single()
        return modelProperties(modelId, model)
    }

    fun enabledModels(): List<EmbeddingModelProperties> =
        modelsById
            .filterValues(EmbeddingModelConfigProperties::enabled)
            .map { (modelId, model) -> modelProperties(modelId, model) }

    private fun modelProperties(
        modelId: String,
        model: EmbeddingModelConfigProperties,
    ): EmbeddingModelProperties {
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

        fun requireRuntimeConfigured(value: String?, propertyName: String): String =
            requireNotNull(value) {
                "aisme.embedding.runtimes.$runtimeId.$propertyName is required for ${runtime.type} embedding models"
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

            EmbeddingModelRuntime.OLLAMA -> EmbeddingModelProperties(
                id = modelId,
                version = version,
                dimensions = dimensions,
                runtime = runtime.type,
                baseUrl = requireRuntimeConfigured(runtime.baseUrl, "base-url"),
                modelName = requireModelConfigured(model.runtime.modelName, "runtime.model-name"),
            )
        }
    }
}
