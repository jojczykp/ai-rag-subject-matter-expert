package org.alterbit.aisme.chatmodel

import org.springframework.stereotype.Component

@Component
class ChatModelRegistry(
    properties: ConfiguredChatModelsProperties,
) {
    private val modelsById: Map<String, ChatModelDescriptor> = properties.chatModels
        .onEachIndexed { index, model -> model.validateRuntimeConfiguration(index) }
        .map { it.toDescriptor() }
        .also { models ->
            require(models.isNotEmpty()) { "aisme.chat-models must contain at least one model" }
            require(models.map { it.id }.distinct().size == models.size) { "aisme.chat-models must not contain duplicate ids" }
        }
        .associateBy { it.id }

    fun chatModels(): List<ChatModelDescriptor> =
        modelsById.values.toList()

    fun findById(modelId: String): ChatModelDescriptor? =
        modelsById[modelId]

    fun getByIdOrThrow(modelId: String): ChatModelDescriptor =
        findById(modelId) ?: throw ChatModelNotFoundException(modelId)

    private fun ConfiguredChatModelProperties.validateRuntimeConfiguration(index: Int) {
        val prefix = "aisme.chat-models[$index]"

        fun requireConfigured(value: String?, propertyName: String) {
            require(value != null) { "$prefix.$propertyName is required for $runtime models" }
        }

        when (runtime) {
            ChatModelRuntime.OLLAMA -> {
                requireConfigured(baseUrl, "base-url")
                requireConfigured(modelName, "model-name")
            }

            ChatModelRuntime.OPENAI_COMPATIBLE -> {
                requireConfigured(baseUrl, "base-url")
                requireConfigured(modelName, "model-name")
                requireConfigured(apiKey, "api-key")
            }

            ChatModelRuntime.HUGGING_FACE_ENDPOINT -> {
                requireConfigured(baseUrl, "base-url")
            }

            ChatModelRuntime.SPRING_AI,
            ChatModelRuntime.EMBEDDED_OFFLINE,
            -> Unit
        }
    }
}
