package org.alterbit.aisme.embedding

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmbeddingClients(
    providers: List<EmbeddingClientProvider>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val clientsByModelId: Map<String, EmbeddingClient> = providers
        .flatMap { it.clients() }
        .also { clients ->
            require(clients.map { it.modelId }.distinct().size == clients.size) {
                "Embedding clients must not contain duplicate model ids"
            }
        }
        .associateBy { it.modelId }
        .also { clients ->
            require(clients.isNotEmpty()) { "At least one embedding client must be configured" }
            logger.info("Registered {} embedding client(s): {}", clients.size, clients.keys.sorted())
        }

    fun all(): List<EmbeddingClient> =
        clientsByModelId.values.toList()

    fun getByModelIdOrDefaultOrThrow(modelId: String?): EmbeddingClient {
        if (modelId != null) {
            return clientsByModelId[modelId] ?: throw EmbeddingModelNotFoundException(modelId)
        }

        require(clientsByModelId.size == 1) {
            "embeddingModelId is required when multiple embedding models are enabled"
        }

        return clientsByModelId.values.single()
    }
}
