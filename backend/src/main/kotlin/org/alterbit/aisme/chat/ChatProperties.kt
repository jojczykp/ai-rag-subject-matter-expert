package org.alterbit.aisme.chat

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.chat")
data class ChatProperties(
    val timeout: Duration = Duration.ofSeconds(60),
    val relevantChunkLimit: Int = 5,
) {
    init {
        require(timeout.isPositive) { "aisme.chat.timeout must be greater than zero" }
        require(relevantChunkLimit > 0) { "aisme.chat.relevant-chunk-limit must be greater than zero" }
    }
}
