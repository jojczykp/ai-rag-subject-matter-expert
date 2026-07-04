package org.alterbit.aisme.chat.openai

import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelChatResponse
import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.toSingleUserPromptText
import org.alterbit.aisme.chatmodel.ChatModelDescriptor

class OpenAiCompatibleAiModelClient(
    private val model: ChatModelDescriptor,
    private val chatApi: OpenAiCompatibleChatApi,
) : AiModelClient {
    override val modelId: String = model.id

    init {
        model.requireOpenAiCompatibleModelName()
    }

    override fun chat(request: AiModelChatRequest): AiModelChatResponse {
        require(request.modelId == model.id) {
            "OpenAI-compatible client for model '${model.id}' cannot handle model '${request.modelId}'"
        }

        val response = chatApi.chat(
            OpenAiCompatibleChatRequest(
                model = model.requireOpenAiCompatibleModelName(),
                messages = listOf(
                    OpenAiCompatibleChatMessage(
                        role = "user",
                        content = request.toSingleUserPromptText(),
                    ),
                ),
            ),
        )
        val answer = response.choices.firstOrNull()
            ?.message
            ?.content
            .orEmpty()
            .trim()

        check(answer.isNotBlank()) {
            "OpenAI-compatible provider returned blank answer for model '${model.id}'"
        }

        return AiModelChatResponse(
            modelId = request.modelId,
            answer = answer,
        )
    }
}

fun ChatModelDescriptor.requireOpenAiCompatibleBaseUrl(): String =
    checkNotNull(baseUrl) { "OpenAI-compatible model '$id' requires baseUrl" }

fun ChatModelDescriptor.requireOpenAiCompatibleModelName(): String =
    checkNotNull(modelName) { "OpenAI-compatible model '$id' requires modelName" }

fun ChatModelDescriptor.requireOpenAiCompatibleApiKey(): String =
    checkNotNull(apiKey) { "OpenAI-compatible model '$id' requires apiKey" }
