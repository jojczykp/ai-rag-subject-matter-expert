package org.alterbit.aisme.chat.api

data class ChatModelsResponseDto(
    val defaultChatModelId: String?,
    val chatApiTimeoutSeconds: Long,
    val chatModels: List<ChatModelDto>,
)
