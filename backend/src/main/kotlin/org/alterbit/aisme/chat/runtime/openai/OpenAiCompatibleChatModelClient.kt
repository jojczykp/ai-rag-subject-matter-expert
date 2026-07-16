package org.alterbit.aisme.chat.runtime.openai

import org.alterbit.aisme.chat.ChatModelRequest
import org.alterbit.aisme.chat.ChatModelResponse
import org.alterbit.aisme.chat.ChatModelClient
import org.alterbit.aisme.chat.toSingleUserPromptText
import org.alterbit.aisme.chat.catalog.ChatModelDescriptor
import org.slf4j.LoggerFactory

class OpenAiCompatibleChatModelClient(
    private val model: ChatModelDescriptor,
    private val chatApi: OpenAiCompatibleChatApi,
) : ChatModelClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    override val modelId: String = model.id

    init {
        model.requireOpenAiCompatibleModelName()
    }

    override fun chat(request: ChatModelRequest): ChatModelResponse {
        require(request.modelId == model.id) {
            "OpenAI-compatible client for model '${model.id}' cannot handle model '${request.modelId}'"
        }

        val providerRequest = OpenAiCompatibleChatRequest(
            model = model.requireOpenAiCompatibleModelName(),
            messages = listOf(
                OpenAiCompatibleChatMessage(
                    role = "user",
                    content = request.toSingleUserPromptText(),
                ),
            ),
        )
        logger.info("Calling OpenAI-compatible provider for model '{}'", model.id)
        val providerResponse = chatApi.chat(providerRequest)
        val answer = providerResponse.choices.firstOrNull()
            ?.message
            ?.content
            .orEmpty()
            .trim()

        check(answer.isNotBlank()) {
            "OpenAI-compatible provider returned blank answer for model '${model.id}'"
        }
        logger.info("OpenAI-compatible provider returned non-blank answer for model '{}'", model.id)

        return ChatModelResponse(
            modelId = request.modelId,
            answer = answer,
        )
    }
}

fun ChatModelDescriptor.requireOpenAiCompatibleBaseUrl(): String =
    checkNotNull(baseUrl) { "OpenAI-compatible model '$id' requires baseUrl" }

fun ChatModelDescriptor.requireOpenAiCompatibleModelName(): String =
    checkNotNull(modelName) { "OpenAI-compatible model '$id' requires modelName" }
