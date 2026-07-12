package org.alterbit.aisme.chat

data class AiModelChatResponse(
    val modelId: String,
    val answer: String,
) {
    init {
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(answer.isNotBlank()) { "answer must not be blank" }
    }
}
