package org.alterbit.aisme.retrieval

import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID
import org.alterbit.aisme.DatabaseTestContext
import org.alterbit.aisme.embedding.EmbeddingModelMetadata
import org.alterbit.aisme.embedding.EmbeddingVector
import org.alterbit.aisme.persistence.ChunkEmbeddingRepository
import org.alterbit.aisme.persistence.DocumentChunkRecord
import org.alterbit.aisme.persistence.DocumentChunkRepository
import org.alterbit.aisme.persistence.SaveChunkEmbeddingRequest
import org.alterbit.aisme.persistence.SourceDocumentRecord
import org.alterbit.aisme.persistence.SourceDocumentRepository
import org.alterbit.aisme.testsupport.addPostgresProperties
import org.alterbit.aisme.testsupport.pgVectorContainer
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(classes = [DatabaseTestContext::class])
class RelevantChunkRetrieverIntegrationTest(
    private val sourceDocumentRepository: SourceDocumentRepository,
    private val documentChunkRepository: DocumentChunkRepository,
    private val chunkEmbeddingRepository: ChunkEmbeddingRepository,
    private val relevantChunkRetriever: RelevantChunkRetriever,
) {
    @Test
    fun `retrieves relevant chunks by pgvector cosine distance`() {
        val sourceDocument = sourceDocumentRepository.save(
            SourceDocumentRecord(
                resourcePath = "culinary_expert/retrieval.txt",
                contentHash = "retrieval-hash",
                indexedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val sourceDocumentId = requireNotNull(sourceDocument.id)

        val closestChunk = saveChunk(
            sourceDocumentId = sourceDocumentId,
            chunkIndex = 0,
            content = "Closest chunk",
        )
        val distantChunk = saveChunk(
            sourceDocumentId = sourceDocumentId,
            chunkIndex = 1,
            content = "Distant chunk",
        )
        val wrongModelChunk = saveChunk(
            sourceDocumentId = sourceDocumentId,
            chunkIndex = 2,
            content = "Wrong model chunk",
        )

        saveEmbedding(
            documentChunkId = requireNotNull(closestChunk.id),
            values = embedding(firstDimension = 1.0),
            embeddingModelId = EMBEDDING_MODEL_ID,
        )
        saveEmbedding(
            documentChunkId = requireNotNull(distantChunk.id),
            values = embedding(secondDimension = 1.0),
            embeddingModelId = EMBEDDING_MODEL_ID,
        )
        saveEmbedding(
            documentChunkId = requireNotNull(wrongModelChunk.id),
            values = embedding(firstDimension = 1.0),
            embeddingModelId = "different-model",
        )

        val chunks = relevantChunkRetriever.retrieve(
            RelevantChunkRequest(
                embedding = embedding(firstDimension = 1.0),
                embeddingModel = EmbeddingModelMetadata(
                    id = EMBEDDING_MODEL_ID,
                    version = EMBEDDING_MODEL_VERSION,
                    dimensions = EMBEDDING_DIMENSIONS,
                ),
                chunkingStrategyVersion = CHUNKING_STRATEGY_VERSION,
                limit = 10,
            ),
        )

        chunks.map { it.content } shouldBe listOf("Closest chunk", "Distant chunk")
        chunks.map { it.resourcePath }.distinct() shouldBe listOf("culinary_expert/retrieval.txt")
    }

    private fun saveChunk(
        sourceDocumentId: UUID,
        chunkIndex: Int,
        content: String,
    ): DocumentChunkRecord =
        documentChunkRepository.save(
            DocumentChunkRecord(
                sourceDocumentId = sourceDocumentId,
                chunkIndex = chunkIndex,
                content = content,
                startOffset = chunkIndex * 100,
                endOffset = chunkIndex * 100 + content.length,
                chunkingStrategyVersion = CHUNKING_STRATEGY_VERSION,
            ),
        )

    private fun saveEmbedding(
        documentChunkId: UUID,
        values: List<Double>,
        embeddingModelId: String,
    ) {
        chunkEmbeddingRepository.save(
            SaveChunkEmbeddingRequest(
                documentChunkId = documentChunkId,
                embedding = EmbeddingVector(
                    values = values,
                    model = EmbeddingModelMetadata(
                        id = embeddingModelId,
                        version = EMBEDDING_MODEL_VERSION,
                        dimensions = EMBEDDING_DIMENSIONS,
                    ),
                ),
                chunkingStrategyVersion = CHUNKING_STRATEGY_VERSION,
                embeddedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
    }

    private fun embedding(
        firstDimension: Double = 0.0,
        secondDimension: Double = 0.0,
    ): List<Double> =
        List(EMBEDDING_DIMENSIONS) { index ->
            when (index) {
                0 -> firstDimension
                1 -> secondDimension
                else -> 0.0
            }
        }

    companion object {
        private const val EMBEDDING_DIMENSIONS = 384
        private const val EMBEDDING_MODEL_ID = "BAAI/bge-small-en-v1.5"
        private const val EMBEDDING_MODEL_VERSION = "test"
        private const val CHUNKING_STRATEGY_VERSION = "character-count-v1"

        @Container
        val postgres = pgVectorContainer()

        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) =
            registry.addPostgresProperties(postgres)
    }
}
