package org.alterbit.aisme.chat

interface AiModelClient {
    val modelId: String

    fun chat(request: AiModelChatRequest): AiModelChatResponse
}
