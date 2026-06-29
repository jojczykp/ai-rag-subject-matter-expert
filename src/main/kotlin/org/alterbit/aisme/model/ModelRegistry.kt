package org.alterbit.aisme.model

import org.springframework.stereotype.Component

@Component
class ModelRegistry(
    properties: ConfiguredModelsProperties,
) {
    private val modelsById: Map<String, ModelDescriptor> = properties.models
        .map { it.toDescriptor() }
        .also { models ->
            require(models.isNotEmpty()) { "aisme.models must contain at least one model" }
            require(models.map { it.id }.distinct().size == models.size) { "aisme.models must not contain duplicate ids" }
        }
        .associateBy { it.id }

    fun models(): List<ModelDescriptor> =
        modelsById.values.toList()

    fun findById(modelId: String): ModelDescriptor? =
        modelsById[modelId]

    fun requireById(modelId: String): ModelDescriptor =
        findById(modelId) ?: throw ModelNotFoundException(modelId)
}
