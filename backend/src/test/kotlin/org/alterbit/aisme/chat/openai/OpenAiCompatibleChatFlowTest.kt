package org.alterbit.aisme.chat.openai

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import java.net.InetSocketAddress
import java.time.Clock
import java.util.concurrent.CopyOnWriteArrayList
import org.alterbit.aisme.api.ApiExceptionHandler
import org.alterbit.aisme.chat.AiChatService
import org.alterbit.aisme.chat.AiModelClients
import org.alterbit.aisme.chat.ChatController
import org.alterbit.aisme.chat.ChatContextRetriever
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityService
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelsProperties
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
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
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@Tag("openai-compatible")
@SpringBootTest(classes = [OpenAiCompatibleChatFlowTestContext::class])
@AutoConfigureMockMvc
class OpenAiCompatibleChatFlowTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `routes application chat request through OpenAI-compatible provider`() {
        recordedRequests.clear()

        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "mock-openai",
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.modelId") {
                value("mock-openai")
            }
            jsonPath("$.answer") {
                value("Mock OpenAI-compatible answer")
            }
        }

        recordedRequests.single().method shouldBe "POST"
        recordedRequests.single().authorization shouldBe "Bearer test-api-key"
        recordedRequests.single().body shouldBe """
            {"model":"mock-chat-model","messages":[{"role":"user","content":"How should I cook rice?"}]}
        """.trimIndent()
    }

    companion object {
        private val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
        private val recordedRequests = CopyOnWriteArrayList<RecordedRequest>()

        init {
            server.createContext("/v1/chat/completions") { exchange ->
                recordedRequests += RecordedRequest(
                    method = exchange.requestMethod,
                    authorization = exchange.requestHeaders.getFirst("Authorization"),
                    body = exchange.requestBody.bufferedReader().use { it.readText() },
                )
                exchange.respondJson(
                    """
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "Mock OpenAI-compatible answer"
                              }
                            }
                          ]
                        }
                    """.trimIndent(),
                )
            }
            server.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun openAiCompatibleProperties(registry: DynamicPropertyRegistry) {
            registry.add("aisme.chat-runtimes.openai-compatible.type") { "OPENAI_COMPATIBLE" }
            registry.add("aisme.chat-runtimes.openai-compatible.base-url") { "http://localhost:${server.address.port}/v1" }
            registry.add("aisme.chat-runtimes.openai-compatible.api-key") { "test-api-key" }
            registry.add("aisme.chat-models.mock-openai.enabled") { "true" }
            registry.add("aisme.chat-models.mock-openai.display-name") { "Mock OpenAI-Compatible Model" }
            registry.add("aisme.chat-models.mock-openai.runtime.id") { "openai-compatible" }
            registry.add("aisme.chat-models.mock-openai.runtime.model-name") { "mock-chat-model" }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop(0)
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
    OpenAiCompatibleAiModelClientProvider::class,
    RestClientOpenAiCompatibleChatApiFactory::class,
)
class OpenAiCompatibleChatFlowTestContext {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun chatContextRetriever(): ChatContextRetriever =
        ChatContextRetriever { emptyList() }
}

private fun HttpExchange.respondJson(body: String) {
    responseHeaders.add("Content-Type", "application/json")
    sendResponseHeaders(200, body.toByteArray().size.toLong())
    responseBody.use { it.write(body.toByteArray()) }
}

private data class RecordedRequest(
    val method: String,
    val authorization: String?,
    val body: String,
)
