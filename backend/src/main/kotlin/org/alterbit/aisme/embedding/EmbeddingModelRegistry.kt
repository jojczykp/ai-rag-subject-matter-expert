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
                displayName = model.displayName ?: modelId,
                runtime = runtime.type,
                version = model.version,
                dimensions = model.dimensions,
            )
        }
        .sortedBy { it.id }
        .also { models ->
            logger.info("Configured {} embedding model(s)", models.size)
        }

    fun embeddingModels(): List<EmbeddingModelDescriptor> =
        embeddingModels

    fun enabledEmbeddingModelProperties(): List<EmbeddingModelProperties> =
        properties.enabledModels()
}
