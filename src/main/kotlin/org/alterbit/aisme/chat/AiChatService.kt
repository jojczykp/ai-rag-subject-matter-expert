package org.alterbit.aisme.chat

import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.springframework.stereotype.Service

@Service
class AiChatService(
    private val chatModelRegistry: ChatModelRegistry,
    private val chatProperties: ChatProperties,
    aiModelClients: List<AiModelClient>,
) {
    private val aiModelClientsByModelId: Map<String, AiModelClient> = aiModelClients
        .also { clients ->
            require(clients.map { it.modelId }.distinct().size == clients.size) {
                "AI model clients must not contain duplicate model ids"
            }
        }
        .associateBy { it.modelId }

    fun chat(request: ChatRequestDto): ChatResponseDto {
        val chatModel = chatModelRegistry.getByIdOrThrow(request.modelId)

        val modelResponse = aiModelClientByModelIdOrThrow(chatModel.id).chat(
            AiModelChatRequest(
                modelId = chatModel.id,
                message = request.message,
                contextChunks = emptyList(),
                timeout = chatProperties.timeout,
            ),
        )

        return ChatResponseDto(
            modelId = modelResponse.modelId,
            answer = modelResponse.answer,
        )
    }

    private fun aiModelClientByModelIdOrThrow(modelId: String): AiModelClient =
        aiModelClientsByModelId[modelId] ?: throw AiModelClientNotFoundException(modelId)
}
