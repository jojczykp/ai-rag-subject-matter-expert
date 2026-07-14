package org.alterbit.aisme.chat.huggingface

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import java.net.InetSocketAddress
import java.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class HuggingFaceTgiChatApiFactoryTest {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `sends generate request to TGI endpoint`() {
        val recordedRequests = mutableListOf<RecordedRequest>()
        server.createContext("/generate") { exchange ->
            recordedRequests += RecordedRequest(
                method = exchange.requestMethod,
                authorization = exchange.requestHeaders.getFirst("Authorization"),
                body = exchange.requestBody.bufferedReader().use { it.readText() },
            )
            exchange.respondJson("""{"generated_text":"TGI answer"}""")
        }
        server.start()
        val api = RestClientHuggingFaceTgiChatApiFactory().create(
            baseUrl = "http://localhost:${server.address.port}",
            apiKey = "test-api-key",
            apiTimeout = Duration.ofSeconds(5),
        )

        val response = api.generate(
            HuggingFaceTgiGenerateRequest(inputs = "How should I cook rice?"),
        )

        response.generatedText shouldBe "TGI answer"
        recordedRequests.single().method shouldBe "POST"
        recordedRequests.single().authorization shouldBe "Bearer test-api-key"
        recordedRequests.single().body shouldBe """{"inputs":"How should I cook rice?"}"""
    }

    @Test
    fun `does not send authorization header when api key is not configured`() {
        val recordedRequests = mutableListOf<RecordedRequest>()
        server.createContext("/generate") { exchange ->
            recordedRequests += RecordedRequest(
                method = exchange.requestMethod,
                authorization = exchange.requestHeaders.getFirst("Authorization"),
                body = exchange.requestBody.bufferedReader().use { it.readText() },
            )
            exchange.respondJson("""{"generated_text":"Local TGI answer"}""")
        }
        server.start()
        val api = RestClientHuggingFaceTgiChatApiFactory().create(
            baseUrl = "http://localhost:${server.address.port}",
            apiKey = null,
            apiTimeout = Duration.ofSeconds(5),
        )

        api.generate(HuggingFaceTgiGenerateRequest(inputs = "Hello"))

        recordedRequests.single().authorization shouldBe null
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
