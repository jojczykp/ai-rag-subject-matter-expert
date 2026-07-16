package org.alterbit.aisme.chat

import org.alterbit.aisme.chat.catalog.ChatProperties
import org.alterbit.aisme.document.SubjectsProperties
import org.alterbit.aisme.embedding.EmbeddingClients
import org.alterbit.aisme.retrieval.RelevantChunk
import org.alterbit.aisme.retrieval.RelevantChunkRequest
import org.alterbit.aisme.retrieval.RelevantChunkRetriever
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

fun interface ChatContextRetriever {
    fun retrieve(
        subjectId: String,
        message: String,
        embeddingModelId: String?,
    ): List<ChatModelContextChunk>
}

@Component
class RelevantChatContextRetriever(
    private val chatProperties: ChatProperties,
    private val subjectsProperties: SubjectsProperties,
    private val embeddingClients: EmbeddingClients,
    private val relevantChunkRetriever: RelevantChunkRetriever,
) : ChatContextRetriever {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun retrieve(
        subjectId: String,
        message: String,
        embeddingModelId: String?,
    ): List<ChatModelContextChunk> {
        logger.info(
            "Retrieving chat context for subject '{}' with embedding model '{}' and chunk limit {}",
            subjectId,
            embeddingModelId ?: "<default>",
            chatProperties.retrievedChunkLimit,
        )
        val embeddingClient = embeddingClients.getByModelIdOrDefaultOrThrow(embeddingModelId)
        val embedding = embeddingClient.embed(message)
        val documentsProperties = subjectsProperties.documentsForSubjectOrThrow(subjectId)
        val chunks = relevantChunkRetriever
            .retrieve(
                RelevantChunkRequest(
                    subjectId = subjectId,
                    embedding = embedding.values,
                    embeddingModel = embedding.model,
                    chunkingStrategyVersion = documentsProperties.chunkingStrategyVersion(),
                    limit = chatProperties.retrievedChunkLimit,
                ),
            )
            .map { it.toContextChunk() }
        if (chunks.isEmpty()) {
            logger.warn(
                "Retrieved no relevant chat context chunks for subject '{}' using embedding model '{}'",
                subjectId,
                embedding.model.id,
            )
        } else {
            logger.info(
                "Retrieved {} relevant chat context chunk(s) for subject '{}' using embedding model '{}'",
                chunks.size,
                subjectId,
                embedding.model.id,
            )
        }
        return chunks
    }

    private fun RelevantChunk.toContextChunk(): ChatModelContextChunk =
        ChatModelContextChunk(
            content = content,
            resourcePath = resourcePath,
            chunkIndex = chunkIndex,
        )
}
