package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.alterbit.aisme.embedding.EmbeddingClient
import org.alterbit.aisme.embedding.EmbeddingClientProvider
import org.alterbit.aisme.embedding.EmbeddingClients
import org.alterbit.aisme.embedding.catalog.EmbeddingModelMetadata
import org.alterbit.aisme.embedding.catalog.EmbeddingModelProperties
import org.alterbit.aisme.embedding.EmbeddingVector
import org.alterbit.aisme.DatabaseTestContext
import org.alterbit.aisme.persistence.ChunkEmbeddingRepository
import org.alterbit.aisme.persistence.DocumentChunkRepository
import org.alterbit.aisme.persistence.SourceDocumentRepository
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
class SubjectDocumentIndexerIntegrationTest(
    private val sourceDocumentRepository: SourceDocumentRepository,
    private val documentChunkRepository: DocumentChunkRepository,
    private val chunkEmbeddingRepository: ChunkEmbeddingRepository,
    private val jdbcClient: JdbcClient,
) {
    @Test
    fun `indexes missing embeddings and skips current embeddings`() {
        val embeddingClient = FakeEmbeddingClient(EmbeddingModelProperties().metadata)
        val indexer = indexer(embeddingClient = embeddingClient)
        val chunks = listOf(
            chunk(documentPath = "culinary_expert/indexer-missing.txt", index = 0, content = "First chunk"),
            chunk(documentPath = "culinary_expert/indexer-missing.txt", index = 1, content = "Second chunk"),
        )

        indexer.index(chunks)
        indexer.index(chunks)

        embeddingClient.embeddedTexts shouldBe listOf("First chunk", "Second chunk")
        rowCount("source_document", "resource_path = 'culinary_expert/indexer-missing.txt'") shouldBe 1
        rowCount(
            table = "document_chunk dc JOIN source_document sd ON sd.id = dc.source_document_id",
            where = "sd.resource_path = 'culinary_expert/indexer-missing.txt'",
        ) shouldBe 2
        rowCount(
            table = """
                chunk_embedding ce
                JOIN document_chunk dc ON dc.id = ce.document_chunk_id
                JOIN source_document sd ON sd.id = dc.source_document_id
            """,
            where = "sd.resource_path = 'culinary_expert/indexer-missing.txt'",
        ) shouldBe 2
    }

    @Test
    fun `re-indexes stale embeddings when embedding model version changes`() {
        val chunks = listOf(
            chunk(documentPath = "culinary_expert/indexer-stale.txt", index = 0, content = "Versioned chunk"),
        )
        indexer(
            embeddingClient = FakeEmbeddingClient(EmbeddingModelProperties(version = "old-version").metadata),
        ).index(chunks)

        val replacementEmbeddingClient = FakeEmbeddingClient(EmbeddingModelProperties(version = "new-version").metadata)
        indexer(
            embeddingClient = replacementEmbeddingClient,
        ).index(chunks)

        replacementEmbeddingClient.embeddedTexts shouldBe listOf("Versioned chunk")
        storedEmbeddingVersions("culinary_expert/indexer-stale.txt") shouldBe listOf("new-version")
    }

    @Test
    fun `indexes embeddings for every enabled embedding client`() {
        val firstEmbeddingClient = FakeEmbeddingClient(EmbeddingModelProperties(id = "first-model").metadata)
        val secondEmbeddingClient = FakeEmbeddingClient(EmbeddingModelProperties(id = "second-model").metadata)
        val chunks = listOf(
            chunk(documentPath = "culinary_expert/indexer-multiple.txt", index = 0, content = "Shared chunk"),
        )

        indexer(embeddingClients = listOf(firstEmbeddingClient, secondEmbeddingClient)).index(chunks)

        firstEmbeddingClient.embeddedTexts shouldBe listOf("Shared chunk")
        secondEmbeddingClient.embeddedTexts shouldBe listOf("Shared chunk")
        storedEmbeddingModelIds("culinary_expert/indexer-multiple.txt") shouldBe listOf("first-model", "second-model")
    }

    private fun indexer(
        embeddingClient: FakeEmbeddingClient = FakeEmbeddingClient(EmbeddingModelProperties().metadata),
        embeddingClients: List<FakeEmbeddingClient> = listOf(embeddingClient),
    ): SubjectDocumentIndexer =
        SubjectDocumentIndexer(
            documentsProperties = SubjectDocumentsProperties(),
            sourceDocumentRepository = sourceDocumentRepository,
            documentChunkRepository = documentChunkRepository,
            chunkEmbeddingRepository = chunkEmbeddingRepository,
            embeddingClients = EmbeddingClients(
                providers = listOf(
                    EmbeddingClientProvider { embeddingClients },
                ),
            ),
        )

    private fun chunk(
        documentPath: String,
        index: Int,
        content: String,
    ): SubjectDocumentChunk =
        SubjectDocumentChunk(
            documentPath = documentPath,
            index = index,
            content = content,
            startOffset = index * 100,
            endOffset = index * 100 + content.length,
        )

    private fun rowCount(
        table: String,
        where: String,
    ): Int =
        jdbcClient
            .sql("SELECT COUNT(*) FROM $table WHERE $where")
            .query(Int::class.java)
            .single()

    private fun storedEmbeddingVersions(resourcePath: String): List<String> =
        jdbcClient
            .sql(
                """
                SELECT ce.embedding_model_version
                FROM chunk_embedding ce
                JOIN document_chunk dc ON dc.id = ce.document_chunk_id
                JOIN source_document sd ON sd.id = dc.source_document_id
                WHERE sd.resource_path = :resourcePath
                ORDER BY dc.chunk_index
                """,
            )
            .param("resourcePath", resourcePath)
            .query(String::class.java)
            .list()
            .map(::requireNotNull)

    private fun storedEmbeddingModelIds(resourcePath: String): List<String> =
        jdbcClient
            .sql(
                """
                SELECT ce.embedding_model_id
                FROM chunk_embedding ce
                JOIN document_chunk dc ON dc.id = ce.document_chunk_id
                JOIN source_document sd ON sd.id = dc.source_document_id
                WHERE sd.resource_path = :resourcePath
                ORDER BY ce.embedding_model_id
                """,
            )
            .param("resourcePath", resourcePath)
            .query(String::class.java)
            .list()
            .map(::requireNotNull)

    private class FakeEmbeddingClient(
        override val model: EmbeddingModelMetadata,
    ) : EmbeddingClient {
        override val modelId: String = model.id

        val embeddedTexts = mutableListOf<String>()

        override fun embed(text: String): EmbeddingVector {
            embeddedTexts += text
            return EmbeddingVector(
                values = List(384) { index -> if (index == 0) 1.0 else 0.0 },
                model = model,
            )
        }
    }

    companion object {
        @Container
        val postgres = pgVectorContainer()

        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) =
            registry.addPostgresProperties(postgres)
    }
}
