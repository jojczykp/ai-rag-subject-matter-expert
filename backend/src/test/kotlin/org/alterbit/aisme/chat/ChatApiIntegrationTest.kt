package org.alterbit.aisme.chat

import io.kotest.matchers.collections.shouldContainExactly
import java.time.Clock
import java.time.Duration
import org.alterbit.aisme.chat.api.ChatExceptionHandler
import org.alterbit.aisme.embedding.api.EmbeddingExceptionHandler
import org.alterbit.aisme.web.ApiExceptionHandler
import org.alterbit.aisme.chat.api.ChatController
import org.alterbit.aisme.chat.api.ChatModelsController
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityService
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
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
    classes = [ChatApiIntegrationTestContext::class],
    properties = [
        "aisme.chat.default-model-id=local-ollama-llama",
        "aisme.chat.models.local-ollama-llama.enabled=true",
        "aisme.chat.models.local-ollama-llama.display-order=10",
        "aisme.chat.models.local-ollama-llama.display-name=Local Ollama Llama",
        "aisme.chat.models.local-ollama-llama.runtime.id=local-ollama",
        "aisme.chat.models.local-ollama-llama.runtime.model-name=llama3.2",
        "aisme.chat.models.cloud-gpt.enabled=true",
        "aisme.chat.models.cloud-gpt.display-order=20",
        "aisme.chat.models.cloud-gpt.display-name=Cloud GPT",
        "aisme.chat.models.cloud-gpt.runtime.id=spring-ai",
        "aisme.chat.models.embedded-qwen-0-5b.enabled=false",
        "aisme.chat.models.embedded-qwen-1-5b.enabled=false",
        "aisme.chat.models.embedded-qwen-3b.enabled=false",
        "aisme.chat.models.embedded-mistral-7b.enabled=false",
        "aisme.chat.models.openai-compatible-example.enabled=false",
        "aisme.chat.models.hugging-face-tgi-example.enabled=false",
    ],
)
@AutoConfigureMockMvc
class ChatApiIntegrationTest(
    private val mockMvc: MockMvc,
    @Qualifier("localAiModelClient")
    private val localAiModelClient: FakeAiModelClient,
    @Qualifier("cloudAiModelClient")
    private val cloudAiModelClient: FakeAiModelClient,
) {
    @Test
    fun `lists configured models`() {
        mockMvc.get("/chat-models")
            .andExpect {
                status { isOk() }
                jsonPath("$.chatModels.length()") {
                    value(2)
                }
                jsonPath("$.chatModels[0].id") {
                    value("local-ollama-llama")
                }
                jsonPath("$.chatModels[0].availability") {
                    value("CONFIGURED")
                }
                jsonPath("$.chatModels[0].promptsMayLeaveLocalMachine") {
                    value(false)
                }
                jsonPath("$.chatModels[1].id") {
                    value("cloud-gpt")
                }
                jsonPath("$.chatModels[1].availability") {
                    value("CONFIGURED")
                }
                jsonPath("$.chatModels[1].promptsMayLeaveLocalMachine") {
                    value(true)
                }
            }
    }

    @Test
    fun `routes chat request to selected fake model client`() {
        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "cloud-gpt",
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.modelId") {
                value("cloud-gpt")
            }
            jsonPath("$.answer") {
                value("Fake answer for: How should I cook rice?")
            }
        }

        localAiModelClient.requests shouldContainExactly emptyList()
        cloudAiModelClient.requests shouldContainExactly listOf(
            AiModelChatRequest(
                modelId = "cloud-gpt",
                message = "How should I cook rice?",
                contextChunks = emptyList(),
                apiTimeout = Duration.ofSeconds(60),
            ),
        )
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
    AiModelClients::class,
    ApiExceptionHandler::class,
    ChatExceptionHandler::class,
    EmbeddingExceptionHandler::class,
    ChatController::class,
    ChatModelAvailabilityService::class,
    ChatModelRegistry::class,
    ChatModelsController::class,
    ChatApiIntegrationTestConfiguration::class,
)
class ChatApiIntegrationTestContext

@TestConfiguration
class ChatApiIntegrationTestConfiguration {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun aiModelClientProvider(
        @Qualifier("localAiModelClient")
        localAiModelClient: FakeAiModelClient,
        @Qualifier("cloudAiModelClient")
        cloudAiModelClient: FakeAiModelClient,
    ): AiModelClientProvider =
        AiModelClientProvider {
            listOf(localAiModelClient, cloudAiModelClient)
        }

    @Bean
    fun localAiModelClient(): FakeAiModelClient =
        FakeAiModelClient(modelId = "local-ollama-llama")

    @Bean
    fun cloudAiModelClient(): FakeAiModelClient =
        FakeAiModelClient(modelId = "cloud-gpt")

    @Bean
    fun chatContextRetriever(): ChatContextRetriever =
        ChatContextRetriever { _, _ -> emptyList() }
}
