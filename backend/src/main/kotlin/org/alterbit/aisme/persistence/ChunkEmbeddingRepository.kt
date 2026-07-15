package org.alterbit.aisme.persistence

import java.sql.Timestamp
import java.util.UUID
import org.alterbit.aisme.embedding.catalog.EmbeddingModelMetadata
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component

@Component
class ChunkEmbeddingRepository(
    private val jdbcClient: JdbcClient,
) {
    fun hasCurrentEmbedding(
        documentChunkId: UUID,
        embeddingModel: EmbeddingModelMetadata,
        chunkingStrategyVersion: String,
    ): Boolean {
        require(chunkingStrategyVersion.isNotBlank()) { "chunkingStrategyVersion must not be blank" }

        return jdbcClient
            .sql(HAS_CURRENT_EMBEDDING_SQL)
            .param("documentChunkId", documentChunkId)
            .param("embeddingModelId", embeddingModel.id)
            .param("embeddingModelVersion", embeddingModel.version)
            .param("embeddingDimensions", embeddingModel.dimensions)
            .param("chunkingStrategyVersion", chunkingStrategyVersion)
            .query(Boolean::class.java)
            .single()
    }

    fun save(request: SaveChunkEmbeddingRequest) {
        require(request.embedding.values.isNotEmpty()) { "embedding values must not be empty" }
        require(request.embedding.values.size == request.embedding.model.dimensions) {
            "embedding values size must match embedding dimensions"
        }
        require(request.chunkingStrategyVersion.isNotBlank()) { "chunkingStrategyVersion must not be blank" }

        jdbcClient
            .sql(SAVE_CHUNK_EMBEDDING_SQL)
            .param("documentChunkId", request.documentChunkId)
            .param("embedding", request.embedding.values.toPgVector())
            .param("embeddingModelId", request.embedding.model.id)
            .param("embeddingModelVersion", request.embedding.model.version)
            .param("embeddingDimensions", request.embedding.model.dimensions)
            .param("chunkingStrategyVersion", request.chunkingStrategyVersion)
            .param("embeddedAt", Timestamp.from(request.embeddedAt))
            .update()
    }

    private fun List<Double>.toPgVector(): String =
        joinToString(prefix = "[", postfix = "]", separator = ",")

    companion object {
        private const val SAVE_CHUNK_EMBEDDING_SQL = """
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
            ON CONFLICT (document_chunk_id, embedding_model_id)
            DO UPDATE SET
                embedding = EXCLUDED.embedding,
                embedding_model_version = EXCLUDED.embedding_model_version,
                embedding_dimensions = EXCLUDED.embedding_dimensions,
                chunking_strategy_version = EXCLUDED.chunking_strategy_version,
                embedded_at = EXCLUDED.embedded_at
        """

        private const val HAS_CURRENT_EMBEDDING_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM chunk_embedding
                WHERE document_chunk_id = :documentChunkId
                  AND embedding_model_id = :embeddingModelId
                  AND embedding_model_version = :embeddingModelVersion
                  AND embedding_dimensions = :embeddingDimensions
                  AND chunking_strategy_version = :chunkingStrategyVersion
            )
        """
    }
}
