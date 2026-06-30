package org.alterbit.aisme.chat

import io.kotest.matchers.collections.shouldContainExactly
import java.time.Clock
import java.time.Duration
import org.alterbit.aisme.api.ApiExceptionHandler
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityProperties
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityService
import org.alterbit.aisme.chatmodel.ChatModelRegistry
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
    classes = [ChatApiIntegrationTestContext::class],
    properties = [
        "aisme.chat-models[0].id=local-ollama-llama",
        "aisme.chat-models[0].display-name=Local Ollama Llama",
        "aisme.chat-models[0].runtime=OLLAMA",
        "aisme.chat-models[0].mode=LOCAL_SERVER",
        "aisme.chat-models[0].available-offline=false",
        "aisme.chat-models[0].base-url=http://localhost:11434",
        "aisme.chat-models[1].id=cloud-gpt",
        "aisme.chat-models[1].display-name=Cloud GPT",
        "aisme.chat-models[1].runtime=SPRING_AI",
        "aisme.chat-models[1].mode=ONLINE",
        "aisme.chat-models[1].available-offline=false",
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
        mockMvc.get("/models")
            .andExpect {
                status { isOk() }
                jsonPath("$.models.length()") {
                    value(2)
                }
                jsonPath("$.models[0].id") {
                    value("local-ollama-llama")
                }
                jsonPath("$.models[0].availability") {
                    value("CONFIGURED")
                }
                jsonPath("$.models[0].promptsMayLeaveLocalMachine") {
                    value(false)
                }
                jsonPath("$.models[1].id") {
                    value("cloud-gpt")
                }
                jsonPath("$.models[1].availability") {
                    value("CONFIGURED")
                }
                jsonPath("$.models[1].promptsMayLeaveLocalMachine") {
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
                timeout = Duration.ofSeconds(60),
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
    ConfiguredChatModelsProperties::class,
)
@Import(
    AiChatService::class,
    ApiExceptionHandler::class,
    ChatController::class,
    ChatModelAvailabilityService::class,
    ChatModelRegistry::class,
    ModelsController::class,
    ChatApiIntegrationTestConfiguration::class,
)
class ChatApiIntegrationTestContext

@TestConfiguration
class ChatApiIntegrationTestConfiguration {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun localAiModelClient(): FakeAiModelClient =
        FakeAiModelClient(modelId = "local-ollama-llama")

    @Bean
    fun cloudAiModelClient(): FakeAiModelClient =
        FakeAiModelClient(modelId = "cloud-gpt")
}
