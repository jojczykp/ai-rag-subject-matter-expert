package org.alterbit.aisme.persistence

import io.kotest.matchers.shouldBe
import java.time.Instant
import org.alterbit.aisme.DatabaseTestContext
import org.alterbit.aisme.embedding.catalog.EmbeddingModelMetadata
import org.alterbit.aisme.embedding.EmbeddingVector
import org.alterbit.aisme.testsupport.addPostgresProperties
import org.alterbit.aisme.testsupport.pgVectorContainer
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(classes = [DatabaseTestContext::class])
class ChunkEmbeddingRepositoryIntegrationTest(
    private val jdbcClient: JdbcClient,
    private val sourceDocumentRepository: SourceDocumentRepository,
    private val documentChunkRepository: DocumentChunkRepository,
    private val chunkEmbeddingRepository: ChunkEmbeddingRepository,
) {
    @Test
    fun `stores embedding model metadata and chunking strategy version`() {
        val sourceDocument = sourceDocumentRepository.save(
            SourceDocumentRecord(
                resourcePath = "culinary_expert/embedding-store.txt",
                contentHash = "embedding-store-hash",
                indexedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val chunk = documentChunkRepository.save(
            DocumentChunkRecord(
                sourceDocumentId = requireNotNull(sourceDocument.id),
                chunkIndex = 0,
                content = "Embedding store chunk",
                startOffset = 0,
                endOffset = 21,
                chunkingStrategyVersion = CHUNKING_STRATEGY_VERSION,
            ),
        )

        chunkEmbeddingRepository.save(
            SaveChunkEmbeddingRequest(
                documentChunkId = requireNotNull(chunk.id),
                embedding = EmbeddingVector(
                    values = embedding(firstDimension = 1.0),
                    model = EmbeddingModelMetadata(
                        id = EMBEDDING_MODEL_ID,
                        version = EMBEDDING_MODEL_VERSION,
                        dimensions = EMBEDDING_DIMENSIONS,
                    ),
                ),
                chunkingStrategyVersion = CHUNKING_STRATEGY_VERSION,
                embeddedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val metadata = jdbcClient
            .sql(
                """
                SELECT
                    embedding_model_id,
                    embedding_model_version,
                    embedding_dimensions,
                    chunking_strategy_version,
                    vector_dims(embedding) AS stored_dimensions
                FROM chunk_embedding
                WHERE document_chunk_id = :documentChunkId
                """,
            )
            .param("documentChunkId", chunk.id)
            .query { resultSet, _ ->
                StoredEmbeddingMetadata(
                    modelId = resultSet.getString("embedding_model_id"),
                    modelVersion = resultSet.getString("embedding_model_version"),
                    dimensions = resultSet.getInt("embedding_dimensions"),
                    chunkingStrategyVersion = resultSet.getString("chunking_strategy_version"),
                    storedDimensions = resultSet.getInt("stored_dimensions"),
                )
            }
            .single()

        metadata.modelId shouldBe EMBEDDING_MODEL_ID
        metadata.modelVersion shouldBe EMBEDDING_MODEL_VERSION
        metadata.dimensions shouldBe EMBEDDING_DIMENSIONS
        metadata.chunkingStrategyVersion shouldBe CHUNKING_STRATEGY_VERSION
        metadata.storedDimensions shouldBe EMBEDDING_DIMENSIONS
    }

    @Test
    fun `stores embeddings with different dimensions`() {
        val sourceDocument = sourceDocumentRepository.save(
            SourceDocumentRecord(
                resourcePath = "culinary_expert/variable-dimensions.txt",
                contentHash = "variable-dimensions-hash",
                indexedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val chunk = documentChunkRepository.save(
            DocumentChunkRecord(
                sourceDocumentId = requireNotNull(sourceDocument.id),
                chunkIndex = 0,
                content = "Variable dimension chunk",
                startOffset = 0,
                endOffset = 24,
                chunkingStrategyVersion = CHUNKING_STRATEGY_VERSION,
            ),
        )

        chunkEmbeddingRepository.save(
            SaveChunkEmbeddingRequest(
                documentChunkId = requireNotNull(chunk.id),
                embedding = EmbeddingVector(
                    values = List(NOMIC_EMBEDDING_DIMENSIONS) { index ->
                        if (index == 0) 1.0 else 0.0
                    },
                    model = EmbeddingModelMetadata(
                        id = "ollama-nomic-embed",
                        version = "v1.5",
                        dimensions = NOMIC_EMBEDDING_DIMENSIONS,
                    ),
                ),
                chunkingStrategyVersion = CHUNKING_STRATEGY_VERSION,
                embeddedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val dimensions = jdbcClient
            .sql(
                """
                SELECT vector_dims(embedding)
                FROM chunk_embedding
                WHERE document_chunk_id = :documentChunkId
                """,
            )
            .param("documentChunkId", chunk.id)
            .query(Int::class.java)
            .single()

        dimensions shouldBe NOMIC_EMBEDDING_DIMENSIONS
    }

    private fun embedding(firstDimension: Double): List<Double> =
        List(EMBEDDING_DIMENSIONS) { index ->
            if (index == 0) firstDimension else 0.0
        }

    private data class StoredEmbeddingMetadata(
        val modelId: String,
        val modelVersion: String,
        val dimensions: Int,
        val chunkingStrategyVersion: String,
        val storedDimensions: Int,
    )

    companion object {
        private const val EMBEDDING_DIMENSIONS = 384
        private const val NOMIC_EMBEDDING_DIMENSIONS = 768
        private const val EMBEDDING_MODEL_ID = "local-bge-small"
        private const val EMBEDDING_MODEL_VERSION = "1.5"
        private const val CHUNKING_STRATEGY_VERSION = "character-count-v1:size=700:overlap=100"

        @Container
        val postgres = pgVectorContainer()

        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) =
            registry.addPostgresProperties(postgres)
    }
}
