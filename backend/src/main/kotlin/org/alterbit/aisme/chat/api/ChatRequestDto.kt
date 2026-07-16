package org.alterbit.aisme.chat.api

data class ChatRequestDto(
    val subjectId: String,
    val modelId: String,
    val embeddingModelId: String? = null,
    val message: String,
) {
    init {
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(embeddingModelId == null || embeddingModelId.isNotBlank()) { "embeddingModelId must not be blank" }
        require(message.isNotBlank()) { "message must not be blank" }
    }
}
