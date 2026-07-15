package org.alterbit.aisme.chat.huggingface

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

@Tag("hugging-face-tgi")
@SpringBootTest(classes = [HuggingFaceTgiChatFlowTestContext::class])
@AutoConfigureMockMvc
class HuggingFaceTgiChatFlowTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `routes application chat request through Hugging Face TGI provider`() {
        recordedRequests.clear()

        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "mock-hugging-face",
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.modelId") {
                value("mock-hugging-face")
            }
            jsonPath("$.answer") {
                value("Mock Hugging Face TGI answer")
            }
        }

        recordedRequests.single().method shouldBe "POST"
        recordedRequests.single().authorization shouldBe "Bearer test-api-key"
        recordedRequests.single().body shouldBe """{"inputs":"How should I cook rice?"}"""
    }

    companion object {
        private val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
        private val recordedRequests = CopyOnWriteArrayList<RecordedRequest>()

        init {
            server.createContext("/generate") { exchange ->
                recordedRequests += RecordedRequest(
                    method = exchange.requestMethod,
                    authorization = exchange.requestHeaders.getFirst("Authorization"),
                    body = exchange.requestBody.bufferedReader().use { it.readText() },
                )
                exchange.respondJson("""{"generated_text":"Mock Hugging Face TGI answer"}""")
            }
            server.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun huggingFaceTgiProperties(registry: DynamicPropertyRegistry) {
            registry.add("aisme.chat.runtimes.hugging-face-tgi.type") { "HUGGING_FACE_TGI" }
            registry.add("aisme.chat.runtimes.hugging-face-tgi.base-url") { "http://localhost:${server.address.port}" }
            registry.add("aisme.chat.runtimes.hugging-face-tgi.api-key") { "test-api-key" }
            registry.add("aisme.chat.models.mock-hugging-face.enabled") { "true" }
            registry.add("aisme.chat.models.mock-hugging-face.display-name") { "Mock Hugging Face TGI Model" }
            registry.add("aisme.chat.models.mock-hugging-face.runtime.id") { "hugging-face-tgi" }
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
    HuggingFaceTgiAiModelClientProvider::class,
    RestClientHuggingFaceTgiChatApiFactory::class,
)
class HuggingFaceTgiChatFlowTestContext {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun chatContextRetriever(): ChatContextRetriever =
        ChatContextRetriever { _, _ -> emptyList() }
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
