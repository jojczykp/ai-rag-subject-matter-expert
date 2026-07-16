package org.alterbit.aisme.chat.runtime.huggingface

import org.alterbit.aisme.chat.ChatModelRequest
import org.alterbit.aisme.chat.ChatModelResponse
import org.alterbit.aisme.chat.ChatModelClient
import org.alterbit.aisme.chat.toSingleUserPromptText
import org.alterbit.aisme.chat.catalog.ChatModelDescriptor
import org.slf4j.LoggerFactory

class HuggingFaceTgiChatModelClient(
    private val model: ChatModelDescriptor,
    private val chatApi: HuggingFaceTgiChatApi,
) : ChatModelClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val modelId: String = model.id

    override fun chat(request: ChatModelRequest): ChatModelResponse {
        require(request.modelId == model.id) {
            "Hugging Face TGI client for model '${model.id}' cannot handle model '${request.modelId}'"
        }

        val providerRequest = HuggingFaceTgiGenerateRequest(
            inputs = request.toSingleUserPromptText(),
        )
        logger.info("Calling Hugging Face TGI provider for model '{}'", model.id)
        val providerResponse = chatApi.generate(providerRequest)
        val answer = providerResponse.generatedText.orEmpty().trim()

        check(answer.isNotBlank()) {
            "Hugging Face TGI provider returned blank answer for model '${model.id}'"
        }
        logger.info("Hugging Face TGI provider returned non-blank answer for model '{}'", model.id)

        return ChatModelResponse(
            modelId = request.modelId,
            answer = answer,
        )
    }
}

fun ChatModelDescriptor.requireHuggingFaceTgiBaseUrl(): String =
    checkNotNull(baseUrl) { "Hugging Face TGI model '$id' requires baseUrl" }
