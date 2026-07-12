package org.alterbit.aisme.modelcatalog

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.chat-model-availability")
data class ChatModelAvailabilityProperties(
    val timeout: Duration = Duration.ofSeconds(5),
    val cacheTtl: Duration = Duration.ofSeconds(5),
) {
    init {
        require(timeout.isPositive) {
            "aisme.chat-model-availability.timeout must be greater than zero"
        }
        require(cacheTtl.isPositive) {
            "aisme.chat-model-availability.cache-ttl must be greater than zero"
        }
    }
}
