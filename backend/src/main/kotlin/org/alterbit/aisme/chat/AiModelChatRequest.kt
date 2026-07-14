package org.alterbit.aisme.chat

import java.time.Duration

data class AiModelChatRequest(
    val modelId: String,
    val message: String,
    val contextChunks: List<AiModelContextChunk>,
    val apiTimeout: Duration,
) {
    init {
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(message.isNotBlank()) { "message must not be blank" }
        require(apiTimeout.isPositive) { "apiTimeout must be greater than zero" }
    }
}
