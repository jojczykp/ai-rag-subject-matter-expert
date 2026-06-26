package org.alterbit.aisme.retrieval

import io.kotest.matchers.shouldBe
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import org.alterbit.aisme.persistence.DocumentChunkRecord
import org.alterbit.aisme.persistence.DocumentChunkRepository
import org.alterbit.aisme.persistence.SourceDocumentRecord
import org.alterbit.aisme.persistence.SourceDocumentRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
class RelevantChunkRetrieverIntegrationTest(
    private val jdbcClient: JdbcClient,
    private val sourceDocumentRepository: SourceDocumentRepository,
    private val documentChunkRepository: DocumentChunkRepository,
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
            embedding = embedding(firstDimension = 1.0),
            embeddingModelId = EMBEDDING_MODEL_ID,
        )
        saveEmbedding(
            documentChunkId = requireNotNull(distantChunk.id),
            embedding = embedding(secondDimension = 1.0),
            embeddingModelId = EMBEDDING_MODEL_ID,
        )
        saveEmbedding(
            documentChunkId = requireNotNull(wrongModelChunk.id),
            embedding = embedding(firstDimension = 1.0),
            embeddingModelId = "different-model",
        )

        val chunks = relevantChunkRetriever.retrieve(
            RelevantChunkRequest(
                embedding = embedding(firstDimension = 1.0),
                embeddingModelId = EMBEDDING_MODEL_ID,
                embeddingModelVersion = EMBEDDING_MODEL_VERSION,
                embeddingDimensions = EMBEDDING_DIMENSIONS,
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
        embedding: List<Double>,
        embeddingModelId: String,
    ) {
        jdbcClient
            .sql(
                """
                INSERT INTO chunk_embedding (
                    document_chunk_id,
                    embedding,
                    embedding_model_id,
                    embedding_model_version,
                    embedding_dimensions,
                    chunking_strategy_version,
                    embedded_at
                )
                VALUES (
                    :documentChunkId,
                    CAST(:embedding AS vector),
                    :embeddingModelId,
                    :embeddingModelVersion,
                    :embeddingDimensions,
                    :chunkingStrategyVersion,
                    :embeddedAt
                )
                """,
            )
            .param("documentChunkId", documentChunkId)
            .param("embedding", embedding.toPgVector())
            .param("embeddingModelId", embeddingModelId)
            .param("embeddingModelVersion", EMBEDDING_MODEL_VERSION)
            .param("embeddingDimensions", EMBEDDING_DIMENSIONS)
            .param("chunkingStrategyVersion", CHUNKING_STRATEGY_VERSION)
            .param("embeddedAt", Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")))
            .update()
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

    private fun List<Double>.toPgVector(): String =
        joinToString(prefix = "[", postfix = "]", separator = ",")

    companion object {
        private const val EMBEDDING_DIMENSIONS = 384
        private const val EMBEDDING_MODEL_ID = "BAAI/bge-small-en-v1.5"
        private const val EMBEDDING_MODEL_VERSION = "test"
        private const val CHUNKING_STRATEGY_VERSION = "character-count-v1"

        @Container
        val postgres: PgVectorContainer =
            PgVectorContainer()
                .withDatabaseName("aisme")
                .withUsername("aisme")
                .withPassword("aisme")

        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    class PgVectorContainer : PostgreSQLContainer<PgVectorContainer>("pgvector/pgvector:0.8.2-pg18")
}
