package org.alterbit.aisme.chat

import java.time.Duration

data class AiModelChatRequest(
    val modelId: String,
    val message: String,
    val contextChunks: List<AiModelContextChunk>,
    val timeout: Duration,
) {
    init {
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(message.isNotBlank()) { "message must not be blank" }
        require(!timeout.isNegative && !timeout.isZero) { "timeout must be greater than zero" }
    }
}
