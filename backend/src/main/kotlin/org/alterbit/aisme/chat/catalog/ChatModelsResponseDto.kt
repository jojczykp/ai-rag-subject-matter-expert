package org.alterbit.aisme.chat.catalog

data class ChatModelsResponseDto(
    val defaultChatModelId: String?,
    val chatApiTimeoutSeconds: Long,
    val chatModels: List<ChatModelDto>,
)
