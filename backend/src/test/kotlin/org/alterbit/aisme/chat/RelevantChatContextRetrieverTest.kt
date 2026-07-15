package org.alterbit.aisme.chat

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.alterbit.aisme.document.SubjectDocumentsProperties
import org.alterbit.aisme.embedding.EmbeddingClient
import org.alterbit.aisme.embedding.EmbeddingClientProvider
import org.alterbit.aisme.embedding.EmbeddingClients
import org.alterbit.aisme.embedding.catalog.EmbeddingModelMetadata
import org.alterbit.aisme.embedding.catalog.EmbeddingModelNotFoundException
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
            chatProperties = ChatProperties(retrievedChunkLimit = 3),
            documentsProperties = SubjectDocumentsProperties(chunkSize = 700, chunkOverlap = 100),
            embeddingClients = EmbeddingClients(listOf(EmbeddingClientProvider { listOf(embeddingClient) })),
            relevantChunkRetriever = relevantChunkRetriever,
        )

        val contextChunks = retriever.retrieve(
            message = "How should I cook rice?",
            embeddingModelId = "local-bge-small",
        )

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
            chatProperties = ChatProperties(retrievedChunkLimit = 3),
            documentsProperties = SubjectDocumentsProperties(),
            embeddingClients = EmbeddingClients(
                listOf(
                    EmbeddingClientProvider {
                        listOf(
                            FakeEmbeddingClient(
                                embedding = EmbeddingVector(
                                    values = listOf(0.1),
                                    model = EmbeddingModelMetadata(
                                        id = "local-bge-small",
                                        version = "1.5",
                                        dimensions = 1,
                                    ),
                                ),
                            ),
                        )
                    },
                ),
            ),
            relevantChunkRetriever = FakeRelevantChunkRetriever(chunks = emptyList()),
        )

        val contextChunks = retriever.retrieve(
            message = "Question without matching chunks",
            embeddingModelId = null,
        )

        contextChunks shouldContainExactly emptyList()
    }

    @Test
    fun `uses selected embedding model for retrieval`() {
        val firstEmbeddingClient = FakeEmbeddingClient(
            embedding = EmbeddingVector(
                values = listOf(0.1),
                model = EmbeddingModelMetadata(
                    id = "first-model",
                    version = "1.0",
                    dimensions = 1,
                ),
            ),
        )
        val secondEmbeddingClient = FakeEmbeddingClient(
            embedding = EmbeddingVector(
                values = listOf(0.9),
                model = EmbeddingModelMetadata(
                    id = "second-model",
                    version = "1.0",
                    dimensions = 1,
                ),
            ),
        )
        val relevantChunkRetriever = FakeRelevantChunkRetriever(chunks = emptyList())
        val retriever = RelevantChatContextRetriever(
            chatProperties = ChatProperties(retrievedChunkLimit = 3),
            documentsProperties = SubjectDocumentsProperties(),
            embeddingClients = EmbeddingClients(
                listOf(
                    EmbeddingClientProvider { listOf(firstEmbeddingClient, secondEmbeddingClient) },
                ),
            ),
            relevantChunkRetriever = relevantChunkRetriever,
        )

        retriever.retrieve(
            message = "Question",
            embeddingModelId = "second-model",
        )

        firstEmbeddingClient.texts shouldContainExactly emptyList()
        secondEmbeddingClient.texts shouldContainExactly listOf("Question")
        relevantChunkRetriever.requests.single().embeddingModel shouldBe EmbeddingModelMetadata(
            id = "second-model",
            version = "1.0",
            dimensions = 1,
        )
    }

    @Test
    fun `requires selected embedding model when multiple embedding clients are enabled`() {
        val retriever = RelevantChatContextRetriever(
            chatProperties = ChatProperties(retrievedChunkLimit = 3),
            documentsProperties = SubjectDocumentsProperties(),
            embeddingClients = EmbeddingClients(
                listOf(
                    EmbeddingClientProvider {
                        listOf(
                            FakeEmbeddingClient(
                                embedding = EmbeddingVector(
                                    values = listOf(0.1),
                                    model = EmbeddingModelMetadata(
                                        id = "first-model",
                                        version = "1.0",
                                        dimensions = 1,
                                    ),
                                ),
                            ),
                            FakeEmbeddingClient(
                                embedding = EmbeddingVector(
                                    values = listOf(0.2),
                                    model = EmbeddingModelMetadata(
                                        id = "second-model",
                                        version = "1.0",
                                        dimensions = 1,
                                    ),
                                ),
                            ),
                        )
                    },
                ),
            ),
            relevantChunkRetriever = FakeRelevantChunkRetriever(chunks = emptyList()),
        )

        val exception = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
            retriever.retrieve(
                message = "Question",
                embeddingModelId = null,
            )
        }

        exception.message shouldBe "embeddingModelId is required when multiple embedding models are enabled"
    }

    @Test
    fun `rejects unknown selected embedding model`() {
        val retriever = RelevantChatContextRetriever(
            chatProperties = ChatProperties(retrievedChunkLimit = 3),
            documentsProperties = SubjectDocumentsProperties(),
            embeddingClients = EmbeddingClients(
                listOf(
                    EmbeddingClientProvider {
                        listOf(
                            FakeEmbeddingClient(
                                embedding = EmbeddingVector(
                                    values = listOf(0.1),
                                    model = EmbeddingModelMetadata(
                                        id = "local-bge-small",
                                        version = "1.5",
                                        dimensions = 1,
                                    ),
                                ),
                            ),
                        )
                    },
                ),
            ),
            relevantChunkRetriever = FakeRelevantChunkRetriever(chunks = emptyList()),
        )

        val exception = io.kotest.assertions.throwables.shouldThrow<EmbeddingModelNotFoundException> {
            retriever.retrieve(
                message = "Question",
                embeddingModelId = "missing-embedding",
            )
        }

        exception.modelId shouldBe "missing-embedding"
    }

    private class FakeEmbeddingClient(
        private val embedding: EmbeddingVector,
    ) : EmbeddingClient {
        override val modelId: String = embedding.model.id
        override val model: EmbeddingModelMetadata = embedding.model

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
