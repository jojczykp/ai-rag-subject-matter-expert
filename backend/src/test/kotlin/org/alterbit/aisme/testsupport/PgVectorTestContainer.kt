package org.alterbit.aisme.testsupport

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer

class PgVectorTestContainer : PostgreSQLContainer<PgVectorTestContainer>("pgvector/pgvector:0.8.2-pg18")

fun pgVectorContainer(): PgVectorTestContainer =
    PgVectorTestContainer()
        .withDatabaseName("aisme")
        .withUsername("aisme")
        .withPassword("aisme")

fun DynamicPropertyRegistry.addPostgresProperties(container: PostgreSQLContainer<*>) {
    add("spring.datasource.url", container::getJdbcUrl)
    add("spring.datasource.username", container::getUsername)
    add("spring.datasource.password", container::getPassword)
}
