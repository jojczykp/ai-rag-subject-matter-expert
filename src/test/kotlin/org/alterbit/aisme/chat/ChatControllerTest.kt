package org.alterbit.aisme.chat

import java.time.Clock
import org.alterbit.aisme.api.ApiExceptionHandler
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityProperties
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityService
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ConfiguredChatModelsProperties
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
        "aisme.chat-models[0].id=local-ollama-llama",
        "aisme.chat-models[0].display-name=Local Ollama Llama",
        "aisme.chat-models[0].runtime=OLLAMA",
        "aisme.chat-models[0].mode=LOCAL_SERVER",
        "aisme.chat-models[0].available-offline=false",
        "aisme.chat-models[0].base-url=http://localhost:11434",
        "aisme.chat-models[0].model-name=llama3.2",
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
    ConfiguredChatModelsProperties::class,
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
}
