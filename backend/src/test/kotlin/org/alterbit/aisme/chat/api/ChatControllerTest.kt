package org.alterbit.aisme.chat.api

import java.time.Clock
import org.alterbit.aisme.chat.AiChatService
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.AiModelClients
import org.alterbit.aisme.chat.ChatContextRetriever
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chat.FakeAiModelClient
import org.alterbit.aisme.web.ApiExceptionHandler
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityService
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelsProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest(
    classes = [ChatControllerTestContext::class],
    properties = [
        "aisme.chat.models.local-ollama-llama.enabled=true",
        "aisme.chat.models.local-ollama-llama.display-name=Local Ollama Llama",
        "aisme.chat.models.local-ollama-llama.runtime.id=local-ollama",
        "aisme.chat.models.local-ollama-llama.runtime.model-name=llama3.2",
    ],
)
@AutoConfigureMockMvc
class ChatControllerTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `answers chat request with selected model`() {
        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "local-ollama-llama",
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.modelId") {
                value("local-ollama-llama")
            }
            jsonPath("$.answer") {
                value("Fake answer for: How should I cook rice?")
            }
        }
    }

    @Test
    fun `returns consistent error when model id is missing`() {
        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") {
                value("INVALID_REQUEST")
            }
            jsonPath("$.message") {
                value("Request body is invalid.")
            }
            jsonPath("$.details.reason") {
                exists()
            }
        }
    }

    @Test
    fun `returns consistent error when message is blank`() {
        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "local-ollama-llama",
                  "message": " "
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") {
                value("INVALID_REQUEST")
            }
            jsonPath("$.message") {
                value("Request body is invalid.")
            }
            jsonPath("$.details.reason") {
                exists()
            }
        }
    }

    @Test
    fun `returns consistent error when model is not configured`() {
        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "missing-model",
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") {
                value("MODEL_NOT_FOUND")
            }
            jsonPath("$.message") {
                value("Configured chat model was not found.")
            }
            jsonPath("$.details.modelId") {
                value("missing-model")
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
    ChatProperties::class,
    ChatModelAvailabilityProperties::class,
    ChatModelsProperties::class,
)
@Import(
    AiChatService::class,
    AiModelClients::class,
    ApiExceptionHandler::class,
    ChatController::class,
    ChatModelAvailabilityService::class,
    ChatModelRegistry::class,
    ChatControllerTestConfiguration::class,
)
class ChatControllerTestContext

@TestConfiguration
class ChatControllerTestConfiguration {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun aiModelClientProvider(): AiModelClientProvider =
        AiModelClientProvider {
            listOf(FakeAiModelClient(modelId = "local-ollama-llama"))
        }

    @Bean
    fun chatContextRetriever(): ChatContextRetriever =
        ChatContextRetriever { _, _ -> emptyList() }
}
