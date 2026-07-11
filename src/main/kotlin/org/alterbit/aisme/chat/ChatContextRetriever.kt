package org.alterbit.aisme.chat

import org.alterbit.aisme.document.SubjectDocumentsProperties
import org.alterbit.aisme.embedding.EmbeddingClient
import org.alterbit.aisme.retrieval.RelevantChunk
import org.alterbit.aisme.retrieval.RelevantChunkRequest
import org.alterbit.aisme.retrieval.RelevantChunkRetriever
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
    override fun retrieve(message: String): List<AiModelContextChunk> {
        val embedding = embeddingClient.embed(message)
        return relevantChunkRetriever
            .retrieve(
                RelevantChunkRequest(
                    embedding = embedding.values,
                    embeddingModel = embedding.model,
                    chunkingStrategyVersion = documentsProperties.chunkingStrategyVersion(),
                    limit = chatProperties.relevantChunkLimit,
                ),
            )
            .map { it.toContextChunk() }
    }

    private fun RelevantChunk.toContextChunk(): AiModelContextChunk =
        AiModelContextChunk(
            content = content,
            resourcePath = resourcePath,
            chunkIndex = chunkIndex,
        )
}
