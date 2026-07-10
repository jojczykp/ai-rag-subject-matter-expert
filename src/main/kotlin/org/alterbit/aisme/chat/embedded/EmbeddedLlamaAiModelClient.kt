package org.alterbit.aisme.chat.embedded

import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelChatResponse
import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.toSingleUserPromptText
import org.alterbit.aisme.chatmodel.ChatModelDescriptor

class EmbeddedLlamaAiModelClient(
    private val model: ChatModelDescriptor,
    private val runtimeModel: EmbeddedLlamaModelProperties,
    private val chatApi: LlamaServerChatApi,
) : AiModelClient {
    override val modelId: String = model.id

    override fun chat(request: AiModelChatRequest): AiModelChatResponse {
        require(request.modelId == model.id) {
            "Llama runtime client for model '${model.id}' cannot handle model '${request.modelId}'"
        }

        val providerRequest = LlamaServerChatRequest(
            model = runtimeModel.id,
            messages = listOf(
                LlamaServerChatMessage(
                    role = "user",
                    content = request.toSingleUserPromptText(),
                ),
            ),
            stream = false,
        )
        val providerResponse = chatApi.chat(providerRequest)
        val answer = providerResponse.choices.firstOrNull()
            ?.message
            ?.content
            .orEmpty()
            .trim()

        check(answer.isNotBlank()) {
            "Llama runtime returned blank answer for model '${model.id}'"
        }

        return AiModelChatResponse(
            modelId = request.modelId,
            answer = answer,
        )
    }
}
