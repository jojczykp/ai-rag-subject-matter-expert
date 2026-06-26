package org.alterbit.aisme.persistence

import io.kotest.matchers.shouldBe
import java.time.Instant
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
class DatabaseSchemaIntegrationTest(
    private val jdbcClient: JdbcClient,
    private val sourceDocumentRepository: SourceDocumentRepository,
    private val documentChunkRepository: DocumentChunkRepository,
) {
    @Test
    fun `applies migrations to PostgreSQL with pgvector through application startup`() {
        jdbcClient
            .sql("SELECT to_regclass('public.source_document') IS NOT NULL")
            .query(Boolean::class.java)
            .single() shouldBe true

        jdbcClient
            .sql("SELECT vector_dims('[1,2,3]'::vector)")
            .query(Int::class.java)
            .single() shouldBe 3
    }

    @Test
    fun `persists source documents and chunks with Spring Data JDBC`() {
        val sourceDocument = sourceDocumentRepository.save(
            SourceDocumentRecord(
                resourcePath = "culinary_expert/example.txt",
                contentHash = "hash",
                indexedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val sourceDocumentId = requireNotNull(sourceDocument.id)

        val chunk = documentChunkRepository.save(
            DocumentChunkRecord(
                sourceDocumentId = sourceDocumentId,
                chunkIndex = 0,
                content = "Example chunk",
                startOffset = 0,
                endOffset = 13,
                chunkingStrategyVersion = "character-count-v1",
            ),
        )

        sourceDocumentRepository.findByResourcePath("culinary_expert/example.txt")?.id shouldBe sourceDocumentId
        documentChunkRepository.findBySourceDocumentIdOrderByChunkIndex(sourceDocumentId) shouldBe listOf(chunk)
    }

    companion object {
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
