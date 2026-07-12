package org.alterbit.aisme.chat.ollama

import org.springframework.ai.ollama.api.OllamaApi

interface OllamaChatApi {
    fun chat(request: OllamaApi.ChatRequest): OllamaApi.ChatResponse

    fun modelNames(): Set<String>
}
