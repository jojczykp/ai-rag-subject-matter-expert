package org.alterbit.aisme.retrieval

import java.sql.ResultSet
import java.util.UUID
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component

@Component
class JdbcRelevantChunkRetriever(
    private val jdbcClient: JdbcClient,
) : RelevantChunkRetriever {
    override fun retrieve(request: RelevantChunkRequest): List<RelevantChunk> {
        require(request.embedding.isNotEmpty()) { "embedding must not be empty" }
        require(request.limit > 0) { "limit must be greater than 0" }
        require(request.embedding.size == request.embeddingModel.dimensions) {
            "embedding size must match embeddingDimensions"
        }

        return jdbcClient
            .sql(RETRIEVE_RELEVANT_CHUNKS_SQL)
            .param("embedding", request.embedding.toPgVector())
            .param("embeddingModelId", request.embeddingModel.id)
            .param("embeddingModelVersion", request.embeddingModel.version)
            .param("embeddingDimensions", request.embeddingModel.dimensions)
            .param("chunkingStrategyVersion", request.chunkingStrategyVersion)
            .param("limit", request.limit)
            .query(::mapRelevantChunk)
            .list()
    }

    private fun mapRelevantChunk(resultSet: ResultSet, rowNumber: Int): RelevantChunk =
        RelevantChunk(
            chunkId = resultSet.getObject("chunk_id", UUID::class.java),
            sourceDocumentId = resultSet.getObject("source_document_id", UUID::class.java),
            resourcePath = resultSet.getString("resource_path"),
            chunkIndex = resultSet.getInt("chunk_index"),
            content = resultSet.getString("content"),
            startOffset = resultSet.getInt("start_offset"),
            endOffset = resultSet.getInt("end_offset"),
            cosineDistance = resultSet.getDouble("cosine_distance"),
        )

    private fun List<Double>.toPgVector(): String =
        joinToString(prefix = "[", postfix = "]", separator = ",")

    companion object {
        private const val RETRIEVE_RELEVANT_CHUNKS_SQL = """
            SELECT
                dc.id AS chunk_id,
                dc.source_document_id,
                sd.resource_path,
                dc.chunk_index,
                dc.content,
                dc.start_offset,
                dc.end_offset,
                ce.embedding <=> CAST(:embedding AS vector) AS cosine_distance
            FROM chunk_embedding ce
            JOIN document_chunk dc ON dc.id = ce.document_chunk_id
            JOIN source_document sd ON sd.id = dc.source_document_id
            WHERE ce.embedding_model_id = :embeddingModelId
              AND ce.embedding_model_version = :embeddingModelVersion
              AND ce.embedding_dimensions = :embeddingDimensions
              AND ce.chunking_strategy_version = :chunkingStrategyVersion
            ORDER BY cosine_distance ASC, dc.id ASC
            LIMIT :limit
        """
    }
}

fun interface RelevantChunkRetriever {
    fun retrieve(request: RelevantChunkRequest): List<RelevantChunk>
}
