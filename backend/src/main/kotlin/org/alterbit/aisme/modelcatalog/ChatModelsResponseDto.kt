package org.alterbit.aisme.modelcatalog

data class ChatModelsResponseDto(
    val defaultChatModelId: String?,
    val chatModels: List<ChatModelDto>,
)
