package org.alterbit.aisme.chat.ollama

import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelChatResponse
import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.toSingleUserPromptText
import org.alterbit.aisme.modelcatalog.ChatModelDescriptor
import org.springframework.ai.ollama.api.OllamaApi

class OllamaAiModelClient(
    private val model: ChatModelDescriptor,
    private val chatApi: OllamaChatApi,
) : AiModelClient {
    override val modelId: String = model.id

    init {
        model.requireModelName()
    }

    override fun chat(request: AiModelChatRequest): AiModelChatResponse {
        require(request.modelId == model.id) {
            "Ollama client for model '${model.id}' cannot handle model '${request.modelId}'"
        }

        val ollamaRequest = toOllamaRequest(request)
        val ollamaResponse = chatApi.chat(ollamaRequest)
        val answer = ollamaResponse.message().content().orEmpty().trim()

        check(answer.isNotBlank()) {
            "Ollama returned blank answer for model '${model.id}'"
        }

        return AiModelChatResponse(
            modelId = request.modelId,
            answer = answer,
        )
    }

    private fun toOllamaRequest(request: AiModelChatRequest): OllamaApi.ChatRequest =
        OllamaApi.ChatRequest.builder(model.requireModelName())
            .messages(
                listOf(
                    OllamaApi.Message.builder(OllamaApi.Message.Role.USER)
                        .content(request.toSingleUserPromptText())
                        .build(),
                ),
            )
            .stream(false)
            .build()
}

fun ChatModelDescriptor.requireBaseUrl(): String =
    checkNotNull(baseUrl) { "Ollama model '$id' requires baseUrl" }

fun ChatModelDescriptor.requireModelName(): String =
    checkNotNull(modelName) { "Ollama model '$id' requires modelName" }
