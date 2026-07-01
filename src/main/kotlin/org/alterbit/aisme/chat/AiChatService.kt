package org.alterbit.aisme.chat

import org.alterbit.aisme.chatmodel.ChatModelAvailability
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityService
import org.alterbit.aisme.chatmodel.ChatModelDescriptor
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelUnavailableException
import org.springframework.stereotype.Service

@Service
class AiChatService(
    private val chatModelRegistry: ChatModelRegistry,
    private val chatModelAvailabilityService: ChatModelAvailabilityService,
    private val chatProperties: ChatProperties,
    private val aiModelClients: AiModelClients,
) {
    fun chat(request: ChatRequestDto): ChatResponseDto {
        val chatModel = chatModelAvailabilityService
            .withAvailability(chatModelRegistry.getByIdOrThrow(request.modelId))
            .also(::requireCallableModel)

        val modelResponse = aiModelClients.getByModelIdOrThrow(chatModel.id).chat(
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

    private fun requireCallableModel(model: ChatModelDescriptor) {
        if (model.availability == ChatModelAvailability.UNAVAILABLE ||
            model.availability == ChatModelAvailability.MISCONFIGURED
        ) {
            throw ChatModelUnavailableException(
                modelId = model.id,
                availability = model.availability,
            )
        }
    }
}
