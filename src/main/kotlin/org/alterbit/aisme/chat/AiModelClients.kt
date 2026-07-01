package org.alterbit.aisme.chat

import org.springframework.stereotype.Component

@Component
class AiModelClients(
    providers: List<AiModelClientProvider>,
) {
    private val clientsByModelId: Map<String, AiModelClient> = providers
        .flatMap { it.clients() }
        .also { clients ->
            require(clients.map { it.modelId }.distinct().size == clients.size) {
                "AI model clients must not contain duplicate model ids"
            }
        }
        .associateBy { it.modelId }

    fun getByModelIdOrThrow(modelId: String): AiModelClient =
        clientsByModelId[modelId] ?: throw AiModelClientNotFoundException(modelId)
}
