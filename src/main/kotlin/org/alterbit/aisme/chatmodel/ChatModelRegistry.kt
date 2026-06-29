package org.alterbit.aisme.chatmodel

import org.springframework.stereotype.Component

@Component
class ChatModelRegistry(
    properties: ConfiguredChatModelsProperties,
) {
    private val modelsById: Map<String, ChatModelDescriptor> = properties.chatModels
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
}
