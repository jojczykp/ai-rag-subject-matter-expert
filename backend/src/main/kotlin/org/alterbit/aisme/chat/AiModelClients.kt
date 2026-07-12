package org.alterbit.aisme.chat

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AiModelClients(
    providers: List<AiModelClientProvider>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val clientsByModelId: Map<String, AiModelClient> = providers
        .flatMap { it.clients() }
        .also { clients ->
            require(clients.map { it.modelId }.distinct().size == clients.size) {
                "AI model clients must not contain duplicate model ids"
            }
        }
        .associateBy { it.modelId }
        .also { clients ->
            logger.info("Registered {} AI model client(s): {}", clients.size, clients.keys.sorted())
        }

    fun getByModelIdOrThrow(modelId: String): AiModelClient =
        clientsByModelId[modelId] ?: throw AiModelClientNotFoundException(modelId)
}
