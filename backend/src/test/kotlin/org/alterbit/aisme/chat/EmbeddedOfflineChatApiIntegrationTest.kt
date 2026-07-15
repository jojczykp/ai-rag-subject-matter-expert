package org.alterbit.aisme.chat

import io.kotest.matchers.collections.shouldContainExactly
import java.time.Clock
import java.time.Duration
import org.alterbit.aisme.api.ApiExceptionHandler
import org.alterbit.aisme.modelcatalog.ChatModelAvailability
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityChecker
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityService
import org.alterbit.aisme.modelcatalog.ChatModelDescriptor
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.ChatModelsProperties
import org.alterbit.aisme.modelcatalog.ChatModelsController
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
        "aisme.chat.models.embedded-ready.enabled=true",
        "aisme.chat.models.embedded-ready.display-order=10",
        "aisme.chat.models.embedded-ready.display-name=Embedded Ready",
        "aisme.chat.models.embedded-ready.runtime.id=embedded-llama",
        "aisme.chat.models.embedded-ready.runtime.model-name=qwen2.5",
        "aisme.chat.models.embedded-ready.runtime.gguf-file=models/qwen.gguf",
        "aisme.chat.models.embedded-ready.runtime.context-size=2048",
        "aisme.chat.models.embedded-down.enabled=true",
        "aisme.chat.models.embedded-down.display-order=20",
        "aisme.chat.models.embedded-down.display-name=Embedded Down",
        "aisme.chat.models.embedded-down.runtime.id=embedded-llama",
        "aisme.chat.models.embedded-down.runtime.model-name=qwen2.5",
        "aisme.chat.models.embedded-down.runtime.gguf-file=models/qwen.gguf",
        "aisme.chat.models.embedded-down.runtime.context-size=2048",
        "aisme.chat.models.embedded-qwen-0-5b.enabled=false",
        "aisme.chat.models.embedded-qwen-1-5b.enabled=false",
        "aisme.chat.models.embedded-qwen-3b.enabled=false",
        "aisme.chat.models.embedded-mistral-7b.enabled=false",
        "aisme.chat.models.local-ollama-llama.enabled=false",
        "aisme.chat.models.openai-compatible-example.enabled=false",
        "aisme.chat.models.hugging-face-tgi-example.enabled=false",
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
        mockMvc.get("/chat-models")
            .andExpect {
                status { isOk() }
                jsonPath("$.chatModels.length()") {
                    value(2)
                }
                jsonPath("$.chatModels[0].id") {
                    value("embedded-ready")
                }
                jsonPath("$.chatModels[0].runtime") {
                    value("EMBEDDED_LLAMA")
                }
                jsonPath("$.chatModels[0].mode") {
                    value("EMBEDDED_OFFLINE")
                }
                jsonPath("$.chatModels[0].availability") {
                    value("AVAILABLE")
                }
                jsonPath("$.chatModels[0].availableOffline") {
                    value(true)
                }
                jsonPath("$.chatModels[0].promptsMayLeaveLocalMachine") {
                    value(false)
                }
                jsonPath("$.chatModels[1].id") {
                    value("embedded-down")
                }
                jsonPath("$.chatModels[1].availability") {
                    value("UNAVAILABLE")
                }
                jsonPath("$.chatModels[1].availableOffline") {
                    value(true)
                }
                jsonPath("$.chatModels[1].promptsMayLeaveLocalMachine") {
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
                apiTimeout = Duration.ofSeconds(60),
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
    ChatModelsProperties::class,
)
@Import(
    AiChatService::class,
    AiModelClients::class,
    ApiExceptionHandler::class,
    ChatController::class,
    ChatModelAvailabilityService::class,
    ChatModelRegistry::class,
    ChatModelsController::class,
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
                model.runtime == ChatModelRuntime.EMBEDDED_LLAMA

            override fun check(model: ChatModelDescriptor, apiTimeout: Duration): ChatModelAvailability =
                when (model.id) {
                    "embedded-ready" -> ChatModelAvailability.AVAILABLE
                    "embedded-down" -> ChatModelAvailability.UNAVAILABLE
                    else -> ChatModelAvailability.MISCONFIGURED
                }
        }
}
