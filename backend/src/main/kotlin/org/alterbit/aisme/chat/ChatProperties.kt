package org.alterbit.aisme.chat

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.chat")
data class ChatProperties(
    val apiTimeout: Duration = Duration.ofSeconds(60),
    val relevantChunkLimit: Int = 5,
) {
    init {
        require(apiTimeout.isPositive) { "aisme.chat.api-timeout must be greater than zero" }
        require(relevantChunkLimit > 0) { "aisme.chat.relevant-chunk-limit must be greater than zero" }
    }
}
