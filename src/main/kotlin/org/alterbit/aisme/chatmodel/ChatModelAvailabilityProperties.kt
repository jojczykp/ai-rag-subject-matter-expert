package org.alterbit.aisme.chatmodel

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.model-availability")
data class ChatModelAvailabilityProperties(
    val timeout: Duration = Duration.ofSeconds(5),
    val cacheTtl: Duration = Duration.ofSeconds(5),
) {
    init {
        require(timeout.isPositive) {
            "aisme.model-availability.timeout must be greater than zero"
        }
        require(cacheTtl.isPositive) {
            "aisme.model-availability.cache-ttl must be greater than zero"
        }
    }
}
