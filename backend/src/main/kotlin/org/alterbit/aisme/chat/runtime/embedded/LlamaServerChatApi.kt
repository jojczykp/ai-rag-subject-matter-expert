package org.alterbit.aisme.chat.runtime.embedded

interface LlamaServerChatApi {
    fun complete(request: LlamaServerCompletionRequest): LlamaServerCompletionResponse
}

data class LlamaServerCompletionRequest(
    val prompt: String,
    val stream: Boolean,
)

data class LlamaServerCompletionResponse(
    val content: String,
)
