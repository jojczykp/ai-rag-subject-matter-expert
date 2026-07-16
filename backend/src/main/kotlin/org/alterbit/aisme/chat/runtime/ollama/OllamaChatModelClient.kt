package org.alterbit.aisme.chat.runtime.ollama

import org.alterbit.aisme.chat.ChatModelRequest
import org.alterbit.aisme.chat.ChatModelResponse
import org.alterbit.aisme.chat.ChatModelClient
import org.alterbit.aisme.chat.toSingleUserPromptText
import org.alterbit.aisme.chat.catalog.ChatModelDescriptor
import org.slf4j.LoggerFactory
import org.springframework.ai.ollama.api.OllamaApi

class OllamaChatModelClient(
    private val model: ChatModelDescriptor,
    private val chatApi: OllamaChatApi,
) : ChatModelClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    override val modelId: String = model.id

    init {
        model.requireModelName()
    }

    override fun chat(request: ChatModelRequest): ChatModelResponse {
        require(request.modelId == model.id) {
            "Ollama client for model '${model.id}' cannot handle model '${request.modelId}'"
        }

        val ollamaRequest = toOllamaRequest(request)
        logger.info("Calling Ollama provider for model '{}'", model.id)
        val ollamaResponse = chatApi.chat(ollamaRequest)
        val answer = ollamaResponse.message().content().orEmpty().trim()

        check(answer.isNotBlank()) {
            "Ollama returned blank answer for model '${model.id}'"
        }
        logger.info("Ollama provider returned non-blank answer for model '{}'", model.id)

        return ChatModelResponse(
            modelId = request.modelId,
            answer = answer,
        )
    }

    private fun toOllamaRequest(request: ChatModelRequest): OllamaApi.ChatRequest =
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
