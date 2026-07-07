package org.alterbit.aisme.chat.embedded

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import java.net.InetSocketAddress
import java.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class LlamaServerChatApiFactoryTest {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `sends chat completions request to llama server endpoint`() {
        val recordedRequests = mutableListOf<RecordedRequest>()
        server.createContext("/v1/chat/completions") { exchange ->
            recordedRequests += RecordedRequest(
                method = exchange.requestMethod,
                body = exchange.requestBody.bufferedReader().use { it.readText() },
            )
            exchange.respondJson(
                """
                    {
                      "choices": [
                        {
                          "message": {
                            "role": "assistant",
                            "content": "Embedded answer"
                          }
                        }
                      ]
                    }
                """.trimIndent(),
            )
        }
        server.start()
        val api = RestClientLlamaServerChatApiFactory().create(
            baseUrl = "http://localhost:${server.address.port}",
            timeout = Duration.ofSeconds(5),
        )

        val response = api.chat(
            LlamaServerChatRequest(
                model = "llama-runtime-example",
                messages = listOf(
                    LlamaServerChatMessage(
                        role = "user",
                        content = "How should I cook rice?",
                    ),
                ),
                stream = false,
            ),
        )

        response.choices.single().message.content shouldBe "Embedded answer"
        recordedRequests.single().method shouldBe "POST"
        recordedRequests.single().body shouldBe """
            {"model":"llama-runtime-example","messages":[{"role":"user","content":"How should I cook rice?"}],"stream":false}
        """.trimIndent()
    }

    private fun HttpExchange.respondJson(body: String) {
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(200, body.toByteArray().size.toLong())
        responseBody.use { it.write(body.toByteArray()) }
    }

    private data class RecordedRequest(
        val method: String,
        val body: String,
    )
}
