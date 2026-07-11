package org.alterbit.aisme.modelcatalog

import org.springframework.stereotype.Component

@Component
class ChatModelRegistry(
    properties: ConfiguredChatModelsProperties,
) {
    private val modelsById: Map<String, ChatModelDescriptor> = properties.chatModels
        .also { models ->
            require(models.map { it.id }.distinct().size == models.size) {
                "aisme.chat-models must not contain duplicate ids"
            }
        }
        .mapIndexed { index, model -> IndexedConfiguredChatModel(index = index, model = model) }
        .filter { it.model.enabled }
        .onEach { it.model.validateRuntimeConfiguration(it.index) }
        .map { it.model.toDescriptor() }
        .also { models ->
            require(models.isNotEmpty()) { "aisme.chat-models must contain at least one model" }
        }
        .associateBy { it.id }

    fun chatModels(): List<ChatModelDescriptor> =
        modelsById.values.toList()

    fun findById(modelId: String): ChatModelDescriptor? =
        modelsById[modelId]

    fun getByIdOrThrow(modelId: String): ChatModelDescriptor =
        findById(modelId) ?: throw ChatModelNotFoundException(modelId)

    private fun ConfiguredChatModelProperties.validateRuntimeConfiguration(index: Int) {
        val prefix = "aisme.chat-models[$index].config"
        val config = requireEnabledConfig()

        fun requireConfigured(value: String?, propertyName: String) {
            require(value != null) { "$prefix.$propertyName is required for ${config.runtime} models" }
        }

        when (config.runtime) {
            ChatModelRuntime.OLLAMA -> {
                requireConfigured(config.baseUrl, "base-url")
                requireConfigured(config.modelName, "model-name")
            }

            ChatModelRuntime.OPENAI_COMPATIBLE -> {
                requireConfigured(config.baseUrl, "base-url")
                requireConfigured(config.modelName, "model-name")
            }

            ChatModelRuntime.HUGGING_FACE_ENDPOINT -> {
                requireConfigured(config.baseUrl, "base-url")
            }

            ChatModelRuntime.SPRING_AI,
            ChatModelRuntime.EMBEDDED_OFFLINE,
            -> Unit
        }
    }

    private data class IndexedConfiguredChatModel(
        val index: Int,
        val model: ConfiguredChatModelProperties,
    )
}
