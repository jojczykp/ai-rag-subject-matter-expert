package org.alterbit.aisme.chat.runtime.openai

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import java.net.InetSocketAddress
import java.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class OpenAiCompatibleChatApiFactoryTest {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `sends chat completions request to OpenAI-compatible endpoint`() {
        val recordedRequests = mutableListOf<RecordedRequest>()
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
                            "content": "Cloud answer"
                          }
                        }
                      ]
                    }
                """.trimIndent(),
            )
        }
        server.start()
        val api = RestClientOpenAiCompatibleChatApiFactory().create(
            baseUrl = "http://localhost:${server.address.port}/v1",
            apiKey = "test-api-key",
            apiTimeout = Duration.ofSeconds(5),
        )

        val response = api.chat(
            OpenAiCompatibleChatRequest(
                model = "gpt-4.1-mini",
                messages = listOf(
                    OpenAiCompatibleChatMessage(
                        role = "user",
                        content = "How should I cook rice?",
                    ),
                ),
            ),
        )

        response.choices.single().message.content shouldBe "Cloud answer"
        recordedRequests.single().method shouldBe "POST"
        recordedRequests.single().authorization shouldBe "Bearer test-api-key"
        recordedRequests.single().body shouldBe """
            {"model":"gpt-4.1-mini","messages":[{"role":"user","content":"How should I cook rice?"}]}
        """.trimIndent()
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
}
