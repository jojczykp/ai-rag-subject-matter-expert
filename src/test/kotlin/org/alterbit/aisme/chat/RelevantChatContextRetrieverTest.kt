package org.alterbit.aisme.chat

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.alterbit.aisme.document.SubjectDocumentsProperties
import org.alterbit.aisme.embedding.EmbeddingClient
import org.alterbit.aisme.embedding.EmbeddingModelMetadata
import org.alterbit.aisme.embedding.EmbeddingVector
import org.alterbit.aisme.retrieval.RelevantChunk
import org.alterbit.aisme.retrieval.RelevantChunkRequest
import org.alterbit.aisme.retrieval.RelevantChunkRetriever
import org.junit.jupiter.api.Test

class RelevantChatContextRetrieverTest {
    @Test
    fun `embeds user message and maps retrieved chunks to chat context`() {
        val embeddingClient = FakeEmbeddingClient(
            embedding = EmbeddingVector(
                values = listOf(0.1, 0.2, 0.3),
                model = EmbeddingModelMetadata(
                    id = "local-bge-small",
                    version = "1.5",
                    dimensions = 3,
                ),
            ),
        )
        val relevantChunkRetriever = FakeRelevantChunkRetriever(
            chunks = listOf(
                RelevantChunk(
                    chunkId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    sourceDocumentId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    resourcePath = "culinary_expert/rice.txt",
                    chunkIndex = 2,
                    content = "Use two parts water for one part rice.",
                    startOffset = 20,
                    endOffset = 61,
                    cosineDistance = 0.1,
                ),
            ),
        )
        val retriever = RelevantChatContextRetriever(
            chatProperties = ChatProperties(relevantChunkLimit = 3),
            documentsProperties = SubjectDocumentsProperties(chunkSize = 700, chunkOverlap = 100),
            embeddingClient = embeddingClient,
            relevantChunkRetriever = relevantChunkRetriever,
        )

        val contextChunks = retriever.retrieve("How should I cook rice?")

        embeddingClient.texts shouldContainExactly listOf("How should I cook rice?")
        relevantChunkRetriever.requests.single().embedding shouldBe listOf(0.1, 0.2, 0.3)
        relevantChunkRetriever.requests.single().embeddingModel shouldBe EmbeddingModelMetadata(
            id = "local-bge-small",
            version = "1.5",
            dimensions = 3,
        )
        relevantChunkRetriever.requests.single().chunkingStrategyVersion shouldBe "character-count-v1:size=700:overlap=100"
        relevantChunkRetriever.requests.single().limit shouldBe 3
        contextChunks shouldContainExactly listOf(
            AiModelContextChunk(
                content = "Use two parts water for one part rice.",
                resourcePath = "culinary_expert/rice.txt",
                chunkIndex = 2,
            ),
        )
    }

    @Test
    fun `returns empty context when no relevant chunks are found`() {
        val retriever = RelevantChatContextRetriever(
            chatProperties = ChatProperties(relevantChunkLimit = 3),
            documentsProperties = SubjectDocumentsProperties(),
            embeddingClient = FakeEmbeddingClient(
                embedding = EmbeddingVector(
                    values = listOf(0.1),
                    model = EmbeddingModelMetadata(
                        id = "local-bge-small",
                        version = "1.5",
                        dimensions = 1,
                    ),
                ),
            ),
            relevantChunkRetriever = FakeRelevantChunkRetriever(chunks = emptyList()),
        )

        val contextChunks = retriever.retrieve("Question without matching chunks")

        contextChunks shouldContainExactly emptyList()
    }

    private class FakeEmbeddingClient(
        private val embedding: EmbeddingVector,
    ) : EmbeddingClient {
        val texts = mutableListOf<String>()

        override fun embed(text: String): EmbeddingVector {
            texts += text
            return embedding
        }
    }

    private class FakeRelevantChunkRetriever(
        private val chunks: List<RelevantChunk>,
    ) : RelevantChunkRetriever {
        val requests = mutableListOf<RelevantChunkRequest>()

        override fun retrieve(request: RelevantChunkRequest): List<RelevantChunk> {
            requests += request
            return chunks
        }
    }
}
