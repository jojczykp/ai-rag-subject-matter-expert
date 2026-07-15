package org.alterbit.aisme.embedding.api

import java.time.Clock
import org.alterbit.aisme.embedding.catalog.EmbeddingModelAvailabilityProperties
import org.alterbit.aisme.embedding.catalog.EmbeddingModelAvailabilityService
import org.alterbit.aisme.embedding.catalog.EmbeddingModelRegistry
import org.alterbit.aisme.embedding.catalog.EmbeddingProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    classes = [EmbeddingModelsControllerTestContext::class],
)
@AutoConfigureMockMvc
class EmbeddingModelsControllerTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `lists embedding models`() {
        mockMvc.get("/embedding-models")
            .andExpect {
                status { isOk() }
                jsonPath("$.defaultEmbeddingModelId") {
                    value("ollama-nomic-embed")
                }
                jsonPath("$.embeddingApiTimeoutSeconds") {
                    value(60)
                }
                jsonPath("$.embeddingModels.length()") {
                    value(2)
                }
                jsonPath("$.embeddingModels[0].id") {
                    value("local-bge-small")
                }
                jsonPath("$.embeddingModels[0].enabled") {
                    value(true)
                }
                jsonPath("$.embeddingModels[0].displayName") {
                    value("Local BGE Small (1.5, 384d)")
                }
                jsonPath("$.embeddingModels[0].runtime") {
                    value("ONNX")
                }
                jsonPath("$.embeddingModels[0].mode") {
                    value("EMBEDDED_OFFLINE")
                }
                jsonPath("$.embeddingModels[0].availability") {
                    value("CONFIGURED")
                }
                jsonPath("$.embeddingModels[0].version") {
                    value("1.5")
                }
                jsonPath("$.embeddingModels[0].dimensions") {
                    value(384)
                }
                jsonPath("$.embeddingModels[0].availableOffline") {
                    value(true)
                }
                jsonPath("$.embeddingModels[1].id") {
                    value("ollama-nomic-embed")
                }
                jsonPath("$.embeddingModels[1].enabled") {
                    value(true)
                }
                jsonPath("$.embeddingModels[1].runtime") {
                    value("OLLAMA")
                }
                jsonPath("$.embeddingModels[1].mode") {
                    value("LOCAL_SERVER")
                }
                jsonPath("$.embeddingModels[1].availability") {
                    value("CONFIGURED")
                }
                jsonPath("$.embeddingModels[1].availableOffline") {
                    value(false)
                }
            }
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration(
    exclude = [
        DataSourceAutoConfiguration::class,
        FlywayAutoConfiguration::class,
    ],
)
@EnableConfigurationProperties(
    EmbeddingProperties::class,
    EmbeddingModelAvailabilityProperties::class,
)
@Import(
    EmbeddingModelRegistry::class,
    EmbeddingModelAvailabilityService::class,
    EmbeddingModelsController::class,
)
class EmbeddingModelsControllerTestContext {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()
}
