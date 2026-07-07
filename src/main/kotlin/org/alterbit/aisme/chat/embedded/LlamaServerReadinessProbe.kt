package org.alterbit.aisme.chat.embedded

import java.time.Duration
import java.time.Instant
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

fun interface LlamaServerReadinessProbe {
    fun awaitReady(baseUrl: String, timeout: Duration): Boolean
}

@Component
class RestClientLlamaServerReadinessProbe : LlamaServerReadinessProbe {
    override fun awaitReady(baseUrl: String, timeout: Duration): Boolean {
        val deadline = Instant.now().plus(timeout)
        val restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build()

        while (Instant.now().isBefore(deadline)) {
            if (restClient.isHealthy()) {
                return true
            }
            Thread.sleep(READINESS_POLL_INTERVAL.toMillis())
        }

        return false
    }

    private fun RestClient.isHealthy(): Boolean =
        try {
            get()
                .uri("/health")
                .retrieve()
                .toBodilessEntity()
                .statusCode == HttpStatus.OK
        } catch (_: RestClientException) {
            false
        }

    private companion object {
        val READINESS_POLL_INTERVAL: Duration = Duration.ofMillis(250)
    }
}
