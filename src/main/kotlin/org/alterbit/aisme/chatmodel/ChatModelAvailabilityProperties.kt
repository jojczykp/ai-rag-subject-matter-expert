package org.alterbit.aisme.chatmodel

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.model-availability")
data class ChatModelAvailabilityProperties(
    val timeout: Duration = Duration.ofSeconds(5),
) {
    init {
        require(!timeout.isNegative && !timeout.isZero) {
            "aisme.model-availability.timeout must be greater than zero"
        }
    }
}
