package org.alterbit.aisme.chat

data class ChatRequestDto(
    val modelId: String,
    val message: String,
) {
    init {
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(message.isNotBlank()) { "message must not be blank" }
    }
}
