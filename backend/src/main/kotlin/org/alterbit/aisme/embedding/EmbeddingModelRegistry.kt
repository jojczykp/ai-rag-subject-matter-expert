package org.alterbit.aisme.embedding

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmbeddingModelRegistry(
    private val properties: EmbeddingProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val embeddingModels: List<EmbeddingModelDescriptor> = properties.modelsById
        .map { (modelId, model) ->
            val runtimeId = requireNotNull(model.runtime.id) {
                "aisme.embedding.models.$modelId.runtime.id is required"
            }
            val runtime = requireNotNull(properties.runtimesById[runtimeId]) {
                "aisme.embedding.models.$modelId.runtime.id references unknown runtime '$runtimeId'"
            }

            EmbeddingModelDescriptor(
                id = modelId,
                enabled = model.enabled,
                displayOrder = model.displayOrder,
                displayName = model.displayName ?: modelId,
                runtime = runtime.type,
                mode = runtime.type.mode,
                availability = model.availability,
                version = model.version,
                dimensions = model.dimensions,
                baseUrl = runtime.baseUrl,
                modelName = model.runtime.modelName,
                modelPath = model.runtime.modelPath,
                tokenizerPath = model.runtime.tokenizerPath,
            )
        }
        .sortedWith(compareBy<EmbeddingModelDescriptor> { it.displayOrder ?: Int.MAX_VALUE }.thenBy { it.id })
        .also { models ->
            logger.info("Configured {} embedding model(s)", models.size)
        }

    fun embeddingModels(): List<EmbeddingModelDescriptor> =
        embeddingModels

    fun enabledEmbeddingModelProperties(): List<EmbeddingModelProperties> =
        properties.enabledModels()
}

private val EmbeddingModelRuntime.mode: EmbeddingModelMode
    get() = when (this) {
        EmbeddingModelRuntime.ONNX -> EmbeddingModelMode.EMBEDDED_OFFLINE
        EmbeddingModelRuntime.OLLAMA -> EmbeddingModelMode.LOCAL_SERVER
    }

private val EmbeddingModelConfigProperties.availability: EmbeddingModelAvailability
    get() =
        if (enabled) {
            EmbeddingModelAvailability.CONFIGURED
        } else {
            EmbeddingModelAvailability.UNAVAILABLE
        }
