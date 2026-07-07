package org.alterbit.aisme.chat.embedded

interface LlamaServerChatApi {
    fun chat(request: LlamaServerChatRequest): LlamaServerChatResponse
}

data class LlamaServerChatRequest(
    val model: String,
    val messages: List<LlamaServerChatMessage>,
    val stream: Boolean,
)

data class LlamaServerChatMessage(
    val role: String,
    val content: String,
)

data class LlamaServerChatResponse(
    val choices: List<LlamaServerChatChoice> = emptyList(),
)

data class LlamaServerChatChoice(
    val message: LlamaServerChatMessage,
)
