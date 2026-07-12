package org.alterbit.aisme

import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(classes = [ActuatorDatabaseHealthTestContext::class])
@AutoConfigureMockMvc
class ActuatorDatabaseHealthIntegrationTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `actuator health includes database status when database is available`() {
        mockMvc.get("/actuator/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") {
                    value("UP")
                }
                jsonPath("$.components.db.status") {
                    value("UP")
                }
                jsonPath("$.components.db.details.database") {
                    value("PostgreSQL")
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

@SpringBootConfiguration
@EnableAutoConfiguration
class ActuatorDatabaseHealthTestContext
