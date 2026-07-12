package org.alterbit.aisme

import org.alterbit.aisme.testsupport.addPostgresProperties
import org.alterbit.aisme.testsupport.pgVectorContainer
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
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
                jsonPath("$.groups[0]") {
                    value("liveness")
                }
                jsonPath("$.groups[1]") {
                    value("readiness")
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
        val postgres = pgVectorContainer()

        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) =
            registry.addPostgresProperties(postgres)
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
class ActuatorDatabaseHealthTestContext
