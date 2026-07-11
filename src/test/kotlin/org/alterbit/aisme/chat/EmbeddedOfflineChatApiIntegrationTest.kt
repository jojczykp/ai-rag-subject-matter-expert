package org.alterbit.aisme.chat

import io.kotest.matchers.collections.shouldContainExactly
import java.time.Clock
import java.time.Duration
import org.alterbit.aisme.api.ApiExceptionHandler
import org.alterbit.aisme.chatmodel.ChatModelAvailability
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityChecker
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityProperties
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityService
import org.alterbit.aisme.chatmodel.ChatModelDescriptor
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.alterbit.aisme.chatmodel.ConfiguredChatModelsProperties
import org.alterbit.aisme.chatmodel.ModelsController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Qualifier
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(
    classes = [EmbeddedOfflineChatApiIntegrationTestContext::class],
    properties = [
        "aisme.chat-models[0].id=embedded-ready",
        "aisme.chat-models[0].enabled=true",
        "aisme.chat-models[0].config.display-name=Embedded Ready",
        "aisme.chat-models[0].config.runtime=EMBEDDED_OFFLINE",
        "aisme.chat-models[0].config.mode=EMBEDDED_OFFLINE",
        "aisme.chat-models[0].config.available-offline=true",
        "aisme.chat-models[1].id=embedded-down",
        "aisme.chat-models[1].enabled=true",
        "aisme.chat-models[1].config.display-name=Embedded Down",
        "aisme.chat-models[1].config.runtime=EMBEDDED_OFFLINE",
        "aisme.chat-models[1].config.mode=EMBEDDED_OFFLINE",
        "aisme.chat-models[1].config.available-offline=true",
    ],
)
@AutoConfigureMockMvc
class EmbeddedOfflineChatApiIntegrationTest(
    private val mockMvc: MockMvc,
    @Qualifier("embeddedReadyAiModelClient")
    private val embeddedReadyAiModelClient: FakeAiModelClient,
) {
    @Test
    fun `lists embedded models with runtime availability`() {
        mockMvc.get("/models")
            .andExpect {
                status { isOk() }
                jsonPath("$.models.length()") {
                    value(2)
                }
                jsonPath("$.models[0].id") {
                    value("embedded-ready")
                }
                jsonPath("$.models[0].runtime") {
                    value("EMBEDDED_OFFLINE")
                }
                jsonPath("$.models[0].mode") {
                    value("EMBEDDED_OFFLINE")
                }
                jsonPath("$.models[0].availability") {
                    value("AVAILABLE")
                }
                jsonPath("$.models[0].availableOffline") {
                    value(true)
                }
                jsonPath("$.models[0].promptsMayLeaveLocalMachine") {
                    value(false)
                }
                jsonPath("$.models[1].id") {
                    value("embedded-down")
                }
                jsonPath("$.models[1].availability") {
                    value("UNAVAILABLE")
                }
                jsonPath("$.models[1].availableOffline") {
                    value(true)
                }
                jsonPath("$.models[1].promptsMayLeaveLocalMachine") {
                    value(false)
                }
            }
    }

    @Test
    fun `routes chat to available embedded model`() {
        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "embedded-ready",
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.modelId") {
                value("embedded-ready")
            }
            jsonPath("$.answer") {
                value("Fake answer for: How should I cook rice?")
            }
        }

        embeddedReadyAiModelClient.requests shouldContainExactly listOf(
            AiModelChatRequest(
                modelId = "embedded-ready",
                message = "How should I cook rice?",
                contextChunks = emptyList(),
                timeout = Duration.ofSeconds(60),
            ),
        )
    }

    @Test
    fun `rejects chat to unavailable embedded model`() {
        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "embedded-down",
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") {
                value("MODEL_UNAVAILABLE")
            }
            jsonPath("$.message") {
                value("Configured chat model is not available.")
            }
            jsonPath("$.details.modelId") {
                value("embedded-down")
            }
            jsonPath("$.details.availability") {
                value("UNAVAILABLE")
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
    ModelsController::class,
    EmbeddedOfflineChatApiIntegrationTestConfiguration::class,
)
class EmbeddedOfflineChatApiIntegrationTestContext

@TestConfiguration
class EmbeddedOfflineChatApiIntegrationTestConfiguration {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun aiModelClientProvider(
        @Qualifier("embeddedReadyAiModelClient")
        embeddedReadyAiModelClient: FakeAiModelClient,
    ): AiModelClientProvider =
        AiModelClientProvider {
            listOf(embeddedReadyAiModelClient)
        }

    @Bean
    fun embeddedReadyAiModelClient(): FakeAiModelClient =
        FakeAiModelClient(modelId = "embedded-ready")

    @Bean
    fun chatContextRetriever(): ChatContextRetriever =
        ChatContextRetriever { emptyList() }

    @Bean
    fun embeddedAvailabilityChecker(): ChatModelAvailabilityChecker =
        object : ChatModelAvailabilityChecker {
            override fun supports(model: ChatModelDescriptor): Boolean =
                model.runtime == ChatModelRuntime.EMBEDDED_OFFLINE

            override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
                when (model.id) {
                    "embedded-ready" -> ChatModelAvailability.AVAILABLE
                    "embedded-down" -> ChatModelAvailability.UNAVAILABLE
                    else -> ChatModelAvailability.MISCONFIGURED
                }
        }
}
