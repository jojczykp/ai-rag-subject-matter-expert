package org.alterbit.aisme.chat

data class AiModelChatRequest(
    val modelId: String,
    val message: String,
    val contextChunks: List<AiModelContextChunk>,
) {
    init {
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(message.isNotBlank()) { "message must not be blank" }
    }
}
