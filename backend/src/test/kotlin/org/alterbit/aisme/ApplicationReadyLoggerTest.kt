package org.alterbit.aisme

import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.support.GenericApplicationContext

@ExtendWith(OutputCaptureExtension::class)
class ApplicationReadyLoggerTest {
    @Test
    fun `logs when application is ready to accept requests`(output: CapturedOutput) {
        val context = GenericApplicationContext()
        val event = ApplicationReadyEvent(
            SpringApplication(AismeApplication::class.java),
            emptyArray(),
            context,
            Duration.ofMillis(1234),
        )

        ApplicationReadyLogger().logReady(event)

        output.out shouldContain "Application startup work completed in 1234ms; ready to accept requests"
    }
}
