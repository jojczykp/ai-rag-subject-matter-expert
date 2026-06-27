package org.alterbit.aisme.document

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import org.alterbit.aisme.embedding.EmbeddingClient
import org.alterbit.aisme.embedding.EmbeddingModelProperties
import org.alterbit.aisme.persistence.ChunkEmbeddingStore
import org.alterbit.aisme.persistence.DocumentChunkRecord
import org.alterbit.aisme.persistence.DocumentChunkRepository
import org.alterbit.aisme.persistence.SaveChunkEmbeddingRequest
import org.alterbit.aisme.persistence.SourceDocumentRecord
import org.alterbit.aisme.persistence.SourceDocumentRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("!no-db")
class SubjectDocumentIndexer(
    private val documentsProperties: SubjectDocumentsProperties,
    private val embeddingModelProperties: EmbeddingModelProperties,
    private val sourceDocumentRepository: SourceDocumentRepository,
    private val documentChunkRepository: DocumentChunkRepository,
    private val chunkEmbeddingStore: ChunkEmbeddingStore,
    private val embeddingClient: EmbeddingClient,
) {
    @Transactional
    fun index(chunks: List<SubjectDocumentChunk>) {
        chunks
            .groupBy(SubjectDocumentChunk::documentPath)
            .forEach { (documentPath, documentChunks) ->
                indexDocument(documentPath, documentChunks.sortedBy(SubjectDocumentChunk::index))
            }
    }

    private fun indexDocument(
        documentPath: String,
        chunks: List<SubjectDocumentChunk>,
    ) {
        val chunkingStrategyVersion = documentsProperties.chunkingStrategyVersion()
        val contentHash = chunks.contentHash(chunkingStrategyVersion)
        val existingSourceDocument = sourceDocumentRepository.findByResourcePath(documentPath)

        val sourceDocument = when {
            existingSourceDocument == null ->
                sourceDocumentRepository.save(
                    SourceDocumentRecord(
                        resourcePath = documentPath,
                        contentHash = contentHash,
                        indexedAt = Instant.now(),
                    ),
                )

            existingSourceDocument.contentHash != contentHash ->
                sourceDocumentRepository.save(
                    existingSourceDocument.copy(
                        contentHash = contentHash,
                        indexedAt = Instant.now(),
                    ),
                ).also {
                    documentChunkRepository.deleteBySourceDocumentId(requireNotNull(it.id))
                }

            else -> existingSourceDocument
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

        indexedChunks.forEach { chunk ->
            indexEmbeddingIfNeeded(
                chunk = chunk,
                chunkingStrategyVersion = chunkingStrategyVersion,
            )
        }
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
    ) {
        val chunkId = requireNotNull(chunk.id)
        val embeddingModel = embeddingModelProperties.metadata
        if (chunkEmbeddingStore.hasCurrentEmbedding(chunkId, embeddingModel, chunkingStrategyVersion)) {
            return
        }

        val embedding = embeddingClient.embed(chunk.content)
        require(embedding.model == embeddingModel) {
            "embedding model metadata must match configured embedding model"
        }

        chunkEmbeddingStore.save(
            SaveChunkEmbeddingRequest(
                documentChunkId = chunkId,
                embedding = embedding,
                chunkingStrategyVersion = chunkingStrategyVersion,
                embeddedAt = Instant.now(),
            ),
        )
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
}
