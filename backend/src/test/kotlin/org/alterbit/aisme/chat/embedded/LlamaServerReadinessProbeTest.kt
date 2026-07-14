package org.alterbit.aisme.chat.embedded

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.matchers.shouldBe
import java.net.InetSocketAddress
import java.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class LlamaServerReadinessProbeTest {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `returns true when health endpoint returns ok`() {
        server.createContext("/health") { exchange ->
            exchange.respond(status = 200)
        }
        server.start()

        val ready = RestClientLlamaServerReadinessProbe().awaitReady(
            baseUrl = "http://localhost:${server.address.port}",
            apiTimeout = Duration.ofSeconds(1),
        )

        ready shouldBe true
    }

    @Test
    fun `returns false when health endpoint does not become ready`() {
        server.createContext("/health") { exchange ->
            exchange.respond(status = 503)
        }
        server.start()

        val ready = RestClientLlamaServerReadinessProbe().awaitReady(
            baseUrl = "http://localhost:${server.address.port}",
            apiTimeout = Duration.ofMillis(10),
        )

        ready shouldBe false
    }

    private fun HttpExchange.respond(status: Int) {
        sendResponseHeaders(status, -1)
        close()
    }
}
