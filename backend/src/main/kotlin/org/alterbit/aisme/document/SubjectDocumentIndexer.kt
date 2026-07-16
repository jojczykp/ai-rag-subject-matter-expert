package org.alterbit.aisme.document

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import org.alterbit.aisme.embedding.EmbeddingClient
import org.alterbit.aisme.embedding.EmbeddingClients
import org.alterbit.aisme.persistence.ChunkEmbeddingRepository
import org.alterbit.aisme.persistence.DocumentChunkRecord
import org.alterbit.aisme.persistence.DocumentChunkRepository
import org.alterbit.aisme.persistence.SaveChunkEmbeddingRequest
import org.alterbit.aisme.persistence.SourceDocumentRecord
import org.alterbit.aisme.persistence.SourceDocumentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SubjectDocumentIndexer(
    private val sourceDocumentRepository: SourceDocumentRepository,
    private val documentChunkRepository: DocumentChunkRepository,
    private val chunkEmbeddingRepository: ChunkEmbeddingRepository,
    private val embeddingClients: EmbeddingClients,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun index(chunks: List<SubjectDocumentChunk>) {
        val chunksByDocument = chunks.groupBy { chunk -> chunk.subjectId to chunk.documentPath }
        logger.info(
            "Indexing {} static subject document(s) across {} subject(s) with {} chunk(s)",
            chunksByDocument.size,
            chunks.map(SubjectDocumentChunk::subjectId).distinct().size,
            chunks.size,
        )

        chunksByDocument
            .forEach { (documentKey, documentChunks) ->
                val (subjectId, documentPath) = documentKey
                indexDocument(
                    subjectId = subjectId,
                    documentPath = documentPath,
                    chunks = documentChunks.sortedBy(SubjectDocumentChunk::index),
                )
            }
        logger.info("Finished indexing static subject documents")
    }

    private fun indexDocument(
        subjectId: String,
        documentPath: String,
        chunks: List<SubjectDocumentChunk>,
    ) {
        val chunkingStrategyVersion = chunks.chunkingStrategyVersion()
        val contentHash = chunks.contentHash(chunkingStrategyVersion)
        val existingSourceDocument = sourceDocumentRepository.findBySubjectIdAndResourcePath(subjectId, documentPath)

        val sourceDocument = when {
            existingSourceDocument == null -> {
                logger.info(
                    "Indexing new source document '{}' for subject '{}' with {} chunk(s)",
                    documentPath,
                    subjectId,
                    chunks.size,
                )
                sourceDocumentRepository.save(
                    SourceDocumentRecord(
                        subjectId = subjectId,
                        resourcePath = documentPath,
                        contentHash = contentHash,
                        indexedAt = Instant.now(),
                    ),
                )
            }

            existingSourceDocument.contentHash != contentHash -> {
                logger.info(
                    "Re-indexing changed source document '{}' for subject '{}' with {} chunk(s)",
                    documentPath,
                    subjectId,
                    chunks.size,
                )
                sourceDocumentRepository.save(
                    existingSourceDocument.copy(
                        contentHash = contentHash,
                        indexedAt = Instant.now(),
                    ),
                ).also {
                    documentChunkRepository.deleteBySourceDocumentId(requireNotNull(it.id))
                }
            }

            else -> {
                logger.info("Source document '{}' for subject '{}' is unchanged", documentPath, subjectId)
                existingSourceDocument
            }
        }

        val sourceDocumentId = requireNotNull(sourceDocument.id)
        val indexedChunks = documentChunkRepository
            .findBySourceDocumentIdOrderByChunkIndex(sourceDocumentId)
            .takeIf { it.matches(chunks, chunkingStrategyVersion) }
            ?: recreateChunks(
                sourceDocumentId = sourceDocumentId,
                chunks = chunks,
                chunkingStrategyVersion = chunkingStrategyVersion,
            )

        val clients = embeddingClients.all()
        val createdEmbeddingCount = indexedChunks.sumOf { chunk ->
            clients.count { embeddingClient ->
                indexEmbeddingIfNeeded(
                    chunk = chunk,
                    chunkingStrategyVersion = chunkingStrategyVersion,
                    embeddingClient = embeddingClient,
                )
            }
        }
        logger.info(
            "Indexed source document '{}' for subject '{}' with {} chunk(s) using {} embedding model(s); created {} embedding(s)",
            documentPath,
            subjectId,
            indexedChunks.size,
            clients.size,
            createdEmbeddingCount,
        )
    }

    private fun recreateChunks(
        sourceDocumentId: UUID,
        chunks: List<SubjectDocumentChunk>,
        chunkingStrategyVersion: String,
    ): List<DocumentChunkRecord> {
        documentChunkRepository.deleteBySourceDocumentId(sourceDocumentId)

        return chunks.map { chunk ->
            documentChunkRepository.save(
                DocumentChunkRecord(
                    sourceDocumentId = sourceDocumentId,
                    chunkIndex = chunk.index,
                    content = chunk.content,
                    startOffset = chunk.startOffset,
                    endOffset = chunk.endOffset,
                    chunkingStrategyVersion = chunkingStrategyVersion,
                ),
            )
        }
    }

    private fun indexEmbeddingIfNeeded(
        chunk: DocumentChunkRecord,
        chunkingStrategyVersion: String,
        embeddingClient: EmbeddingClient,
    ): Boolean {
        val chunkId = requireNotNull(chunk.id)
        val embeddingModel = embeddingClient.model
        if (chunkEmbeddingRepository.hasCurrentEmbedding(chunkId, embeddingModel, chunkingStrategyVersion)) {
            logger.debug(
                "Current embedding already exists for document chunk '{}' using model '{}:{}'",
                chunkId,
                embeddingModel.id,
                embeddingModel.version,
            )
            return false
        }

        logger.debug(
            "Creating embedding for document chunk '{}' using model '{}:{}'",
            chunkId,
            embeddingModel.id,
            embeddingModel.version,
        )
        val embedding = embeddingClient.embed(chunk.content)
        require(embedding.model == embeddingModel) {
            "embedding model metadata must match selected embedding model"
        }

        chunkEmbeddingRepository.save(
            SaveChunkEmbeddingRequest(
                documentChunkId = chunkId,
                embedding = embedding,
                chunkingStrategyVersion = chunkingStrategyVersion,
                embeddedAt = Instant.now(),
            ),
        )

        return true
    }

    private fun List<DocumentChunkRecord>.matches(
        chunks: List<SubjectDocumentChunk>,
        chunkingStrategyVersion: String,
    ): Boolean =
        size == chunks.size &&
            zip(chunks).all { (stored, current) ->
                stored.chunkIndex == current.index &&
                    stored.content == current.content &&
                    stored.startOffset == current.startOffset &&
                    stored.endOffset == current.endOffset &&
                    stored.chunkingStrategyVersion == chunkingStrategyVersion
            }

    private fun List<SubjectDocumentChunk>.contentHash(chunkingStrategyVersion: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        forEach { chunk ->
            digest.update(chunkingStrategyVersion.toByteArray(StandardCharsets.UTF_8))
            digest.update(0)
            digest.update(chunk.index.toString().toByteArray(StandardCharsets.UTF_8))
            digest.update(0)
            digest.update(chunk.startOffset.toString().toByteArray(StandardCharsets.UTF_8))
            digest.update(0)
            digest.update(chunk.endOffset.toString().toByteArray(StandardCharsets.UTF_8))
            digest.update(0)
            digest.update(chunk.content.toByteArray(StandardCharsets.UTF_8))
            digest.update(0)
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun List<SubjectDocumentChunk>.chunkingStrategyVersion(): String {
        require(isNotEmpty()) { "chunks must not be empty" }
        val versions = map(SubjectDocumentChunk::chunkingStrategyVersion).distinct()
        require(versions.size == 1) { "chunks for one document must use one chunking strategy version" }
        return versions.single()
    }
}
