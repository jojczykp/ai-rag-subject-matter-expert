package org.alterbit.aisme.chat.embedded

import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

fun interface LlamaServerReadinessProbe {
    fun awaitReady(baseUrl: String, apiTimeout: Duration): Boolean
}

@Component
class RestClientLlamaServerReadinessProbe : LlamaServerReadinessProbe {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun awaitReady(baseUrl: String, apiTimeout: Duration): Boolean {
        val deadline = Instant.now().plus(apiTimeout)
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
        } catch (ex: RestClientException) {
            logger.debug("llama-server health check did not succeed yet: '{}'", ex.javaClass.simpleName)
            false
        }

    private companion object {
        val READINESS_POLL_INTERVAL: Duration = Duration.ofMillis(250)
    }
}
