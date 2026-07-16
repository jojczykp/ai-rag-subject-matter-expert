package org.alterbit.aisme.chat

import io.kotest.matchers.collections.shouldContainExactly
import java.time.Clock
import java.time.Duration
import org.alterbit.aisme.chat.api.ChatExceptionHandler
import org.alterbit.aisme.embedding.api.EmbeddingExceptionHandler
import org.alterbit.aisme.web.ApiExceptionHandler
import org.alterbit.aisme.chat.api.ChatController
import org.alterbit.aisme.chat.api.ChatModelsController
import org.alterbit.aisme.chat.catalog.ChatModelAvailability
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityChecker
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityService
import org.alterbit.aisme.chat.catalog.ChatModelDescriptor
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.alterbit.aisme.chat.catalog.ChatModelsProperties
import org.alterbit.aisme.chat.catalog.ChatProperties
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
        "aisme.chat.default-model-id=embedded-ready",
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
    @Qualifier("embeddedReadyChatModelClient")
    private val embeddedReadyChatModelClient: FakeChatModelClient,
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
                  "subjectId": "culinary-expert",
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

        embeddedReadyChatModelClient.requests shouldContainExactly listOf(
            ChatModelRequest(
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
                  "subjectId": "culinary-expert",
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
    ChatService::class,
    ChatModelClients::class,
    ApiExceptionHandler::class,
    ChatExceptionHandler::class,
    EmbeddingExceptionHandler::class,
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
    fun subjectRegistry(): org.alterbit.aisme.document.SubjectRegistry =
        object : org.alterbit.aisme.document.SubjectRegistry {
            override fun subjects(): List<org.alterbit.aisme.document.SubjectDescriptor> =
                listOf(org.alterbit.aisme.testsupport.culinarySubject())

            override fun getByIdOrThrow(subjectId: String): org.alterbit.aisme.document.SubjectDescriptor =
                subjects().first { subject -> subject.id == subjectId }
        }

    @Bean
    fun chatModelClientProvider(
        @Qualifier("embeddedReadyChatModelClient")
        embeddedReadyChatModelClient: FakeChatModelClient,
    ): ChatModelClientProvider =
        ChatModelClientProvider {
            listOf(embeddedReadyChatModelClient)
        }

    @Bean
    fun embeddedReadyChatModelClient(): FakeChatModelClient =
        FakeChatModelClient(modelId = "embedded-ready")

    @Bean
    fun chatContextRetriever(): ChatContextRetriever =
        ChatContextRetriever { _, _, _ -> emptyList() }

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
