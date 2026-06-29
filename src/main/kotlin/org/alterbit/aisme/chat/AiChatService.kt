package org.alterbit.aisme.chat

import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.springframework.stereotype.Service

@Service
class AiChatService(
    private val chatModelRegistry: ChatModelRegistry,
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
        chatModelRegistry.requireById(request.modelId)

        val modelResponse = aiModelClient(request.modelId).chat(
            AiModelChatRequest(
                modelId = request.modelId,
                message = request.message,
                contextChunks = emptyList(),
            ),
        )

        return ChatResponseDto(
            modelId = modelResponse.modelId,
            answer = modelResponse.answer,
        )
    }

    private fun aiModelClient(modelId: String): AiModelClient =
        aiModelClientsByModelId[modelId] ?: throw AiModelClientNotFoundException(modelId)
}
