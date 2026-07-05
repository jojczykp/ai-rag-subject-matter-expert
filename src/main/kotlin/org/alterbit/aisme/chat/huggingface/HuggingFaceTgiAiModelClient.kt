package org.alterbit.aisme.chat.huggingface

import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelChatResponse
import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.toSingleUserPromptText
import org.alterbit.aisme.chatmodel.ChatModelDescriptor

class HuggingFaceTgiAiModelClient(
    private val model: ChatModelDescriptor,
    private val chatApi: HuggingFaceTgiChatApi,
) : AiModelClient {
    override val modelId: String = model.id

    override fun chat(request: AiModelChatRequest): AiModelChatResponse {
        require(request.modelId == model.id) {
            "Hugging Face TGI client for model '${model.id}' cannot handle model '${request.modelId}'"
        }

        val providerRequest = HuggingFaceTgiGenerateRequest(
            inputs = request.toSingleUserPromptText(),
        )
        val providerResponse = chatApi.generate(providerRequest)
        val answer = providerResponse.generatedText.orEmpty().trim()

        check(answer.isNotBlank()) {
            "Hugging Face TGI provider returned blank answer for model '${model.id}'"
        }

        return AiModelChatResponse(
            modelId = request.modelId,
            answer = answer,
        )
    }
}

fun ChatModelDescriptor.requireHuggingFaceTgiBaseUrl(): String =
    checkNotNull(baseUrl) { "Hugging Face TGI model '$id' requires baseUrl" }
