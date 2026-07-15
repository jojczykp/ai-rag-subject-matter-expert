package org.alterbit.aisme.chat.runtime.openai

interface OpenAiCompatibleChatApi {
    fun chat(request: OpenAiCompatibleChatRequest): OpenAiCompatibleChatResponse
}

data class OpenAiCompatibleChatRequest(
    val model: String,
    val messages: List<OpenAiCompatibleChatMessage>,
)

data class OpenAiCompatibleChatMessage(
    val role: String,
    val content: String,
)

data class OpenAiCompatibleChatResponse(
    val choices: List<OpenAiCompatibleChatChoice> = emptyList(),
)

data class OpenAiCompatibleChatChoice(
    val message: OpenAiCompatibleChatMessage,
)
