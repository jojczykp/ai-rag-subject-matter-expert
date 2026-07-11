package org.alterbit.aisme.chat

import org.alterbit.aisme.document.SubjectDocumentsProperties
import org.alterbit.aisme.embedding.EmbeddingClient
import org.alterbit.aisme.retrieval.RelevantChunk
import org.alterbit.aisme.retrieval.RelevantChunkRequest
import org.alterbit.aisme.retrieval.RelevantChunkRetriever
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

fun interface ChatContextRetriever {
    fun retrieve(message: String): List<AiModelContextChunk>
}

@Component
class RelevantChatContextRetriever(
    private val chatProperties: ChatProperties,
    private val documentsProperties: SubjectDocumentsProperties,
    private val embeddingClient: EmbeddingClient,
    private val relevantChunkRetriever: RelevantChunkRetriever,
) : ChatContextRetriever {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun retrieve(message: String): List<AiModelContextChunk> {
        logger.info("Retrieving chat context with chunk limit {}", chatProperties.relevantChunkLimit)
        val embedding = embeddingClient.embed(message)
        val chunks = relevantChunkRetriever
            .retrieve(
                RelevantChunkRequest(
                    embedding = embedding.values,
                    embeddingModel = embedding.model,
                    chunkingStrategyVersion = documentsProperties.chunkingStrategyVersion(),
                    limit = chatProperties.relevantChunkLimit,
                ),
            )
            .map { it.toContextChunk() }
        if (chunks.isEmpty()) {
            logger.warn("Retrieved no relevant chat context chunks")
        } else {
            logger.info("Retrieved {} relevant chat context chunk(s)", chunks.size)
        }
        return chunks
    }

    private fun RelevantChunk.toContextChunk(): AiModelContextChunk =
        AiModelContextChunk(
            content = content,
            resourcePath = resourcePath,
            chunkIndex = chunkIndex,
        )
}
