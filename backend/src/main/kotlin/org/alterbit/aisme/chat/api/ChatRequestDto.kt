package org.alterbit.aisme.chat.api

data class ChatRequestDto(
    val modelId: String,
    val embeddingModelId: String? = null,
    val message: String,
) {
    init {
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(embeddingModelId == null || embeddingModelId.isNotBlank()) { "embeddingModelId must not be blank" }
        require(message.isNotBlank()) { "message must not be blank" }
    }
}
