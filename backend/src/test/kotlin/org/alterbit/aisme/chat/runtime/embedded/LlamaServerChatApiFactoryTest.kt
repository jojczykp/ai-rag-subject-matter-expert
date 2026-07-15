package org.alterbit.aisme.chat.runtime.embedded

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
    fun `sends completion request to llama server endpoint`() {
        val recordedRequests = mutableListOf<RecordedRequest>()
        server.createContext("/completion") { exchange ->
            recordedRequests += RecordedRequest(
                method = exchange.requestMethod,
                body = exchange.requestBody.bufferedReader().use { it.readText() },
            )
            exchange.respondJson(
                """
                    {
                      "content": "Embedded answer"
                    }
                """.trimIndent(),
            )
        }
        server.start()
        val api = RestClientLlamaServerChatApiFactory().create(
            baseUrl = "http://localhost:${server.address.port}",
            apiTimeout = Duration.ofSeconds(5),
        )

        val response = api.complete(
            LlamaServerCompletionRequest(
                prompt = "How should I cook rice?",
                stream = false,
            ),
        )

        response.content shouldBe "Embedded answer"
        recordedRequests.single().method shouldBe "POST"
        recordedRequests.single().body shouldBe """
            {"prompt":"How should I cook rice?","stream":false}
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
