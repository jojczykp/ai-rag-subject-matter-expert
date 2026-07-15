package org.alterbit.aisme.embedding

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.embedding.model-availability")
data class EmbeddingModelAvailabilityProperties(
    val timeout: Duration = Duration.ofSeconds(5),
    val cacheTtl: Duration = Duration.ofSeconds(5),
) {
    init {
        require(timeout.isPositive) {
            "aisme.embedding.model-availability.timeout must be greater than zero"
        }
        require(cacheTtl.isPositive) {
            "aisme.embedding.model-availability.cache-ttl must be greater than zero"
        }
    }
}
