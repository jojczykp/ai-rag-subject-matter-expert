package org.alterbit.aisme.persistence

import io.kotest.matchers.shouldBe
import java.sql.DriverManager
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationPostgresTest {
    @Test
    fun `applies migrations to PostgreSQL with pgvector`() {
        Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .load()
            .migrate()

        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM source_document").use { resultSet ->
                    resultSet.next() shouldBe true
                    resultSet.getInt(1) shouldBe 0
                }

                statement.executeQuery("SELECT vector_dims('[1,2,3]'::vector)").use { resultSet ->
                    resultSet.next() shouldBe true
                    resultSet.getInt(1) shouldBe 3
                }
            }
        }
    }

    companion object {
        @Container
        val postgres: PgVectorContainer =
            PgVectorContainer()
                .withDatabaseName("aisme")
                .withUsername("aisme")
                .withPassword("aisme")
    }

    class PgVectorContainer : PostgreSQLContainer<PgVectorContainer>("pgvector/pgvector:0.8.2-pg18")
}
