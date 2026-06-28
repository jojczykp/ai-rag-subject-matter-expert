package org.alterbit.aisme.chat

interface AiModelClient {
    fun chat(request: AiModelChatRequest): AiModelChatResponse
}
