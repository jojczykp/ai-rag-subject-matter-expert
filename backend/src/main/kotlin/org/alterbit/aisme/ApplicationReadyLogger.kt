package org.alterbit.aisme

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ApplicationReadyLogger {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun logReady(event: ApplicationReadyEvent) {
        val startupDuration = event.timeTaken
            ?.toMillis()
            ?.let { " in ${it}ms" }
            .orEmpty()

        logger.info("Application startup work completed{}; ready to accept requests", startupDuration)
    }
}
