package org.alterbit.aisme.chat

interface ChatModelClient {
    val modelId: String

    fun chat(request: ChatModelRequest): ChatModelResponse
}
