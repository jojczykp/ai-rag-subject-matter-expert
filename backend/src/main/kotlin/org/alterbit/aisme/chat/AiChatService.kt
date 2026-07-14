package org.alterbit.aisme.chat

import java.util.concurrent.CancellationException
import org.alterbit.aisme.modelcatalog.ChatModelAvailability
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityService
import org.alterbit.aisme.modelcatalog.ChatModelDescriptor
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AiChatService(
    private val chatModelRegistry: ChatModelRegistry,
    private val chatModelAvailabilityService: ChatModelAvailabilityService,
    private val chatProperties: ChatProperties,
    private val chatContextRetriever: ChatContextRetriever,
    private val aiModelClients: AiModelClients,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun chat(request: ChatRequestDto): ChatResponseDto {
        val startedAt = System.nanoTime()
        logger.info("Processing chat request for model '{}'", request.modelId)
        val chatModel = chatModelAvailabilityService
            .withAvailability(chatModelRegistry.getByIdOrThrow(request.modelId))
            .also(::requireCallableModel)
        logger.info(
            "Selected chat model '{}' with runtime '{}', mode '{}', and availability '{}'",
            chatModel.id,
            chatModel.runtime,
            chatModel.mode,
            chatModel.availability,
        )

        val contextChunks = chatContextRetriever.retrieve(request.message)
        logger.info("Sending chat request to model '{}' with {} context chunk(s)", chatModel.id, contextChunks.size)

        val modelRequest = AiModelChatRequest(
            modelId = chatModel.id,
            message = request.message,
            contextChunks = contextChunks,
            apiTimeout = chatProperties.apiTimeout,
        )

        val modelResponse = try {
            aiModelClients.getByModelIdOrThrow(chatModel.id).chat(modelRequest)
        } catch (ex: CancellationException) {
            logger.warn("Chat request for model '{}' was cancelled", chatModel.id)
            throw ex
        } catch (ex: AiModelProviderException) {
            logger.warn(
                "Chat provider reported failure for model '{}' and provider '{}'",
                ex.modelId,
                ex.provider,
                ex,
            )
            throw ex
        } catch (ex: AiModelClientNotFoundException) {
            logger.warn("No AI model client found for configured model '{}'", ex.modelId)
            throw ex
        } catch (ex: RuntimeException) {
            logger.warn("Chat provider call failed for model '{}'", chatModel.id, ex)
            throw ex.toAiModelProviderException(
                modelId = chatModel.id,
                provider = chatModel.runtime.providerLabel,
            )
        }

        logger.info(
            "Completed chat request for model '{}' in {} ms",
            chatModel.id,
            elapsedMillis(startedAt),
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
            logger.warn(
                "Rejecting chat request for model '{}' because availability is '{}'",
                model.id,
                model.availability,
            )
            throw ChatModelUnavailableException(
                modelId = model.id,
                availability = model.availability,
            )
        }
    }

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / 1_000_000
}
