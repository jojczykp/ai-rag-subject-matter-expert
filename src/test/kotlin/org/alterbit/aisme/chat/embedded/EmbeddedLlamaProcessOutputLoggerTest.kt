package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import org.junit.jupiter.api.Test

class EmbeddedLlamaProcessOutputLoggerTest {
    @Test
    fun `captures stdout and stderr lines with model id`() {
        val executor = Executors.newFixedThreadPool(2)
        val capturedLines = mutableListOf<CapturedLine>()
        val outputLogger = EmbeddedLlamaProcessOutputLogger(
            executor = executor,
            lineConsumer = { modelId, stream, line ->
                capturedLines += CapturedLine(
                    modelId = modelId,
                    stream = stream,
                    line = line,
                )
            },
        )

        outputLogger.attach(
            modelId = "embedded-llama",
            process = FakeProcess(
                stdout = "server started\nmodel loaded\n",
                stderr = "warning: slow startup\n",
            ),
        ).forEach { it.get() }
        outputLogger.stop()

        capturedLines shouldContainExactlyInAnyOrder listOf(
            CapturedLine(
                modelId = "embedded-llama",
                stream = EmbeddedLlamaProcessOutputStream.STDOUT,
                line = "server started",
            ),
            CapturedLine(
                modelId = "embedded-llama",
                stream = EmbeddedLlamaProcessOutputStream.STDOUT,
                line = "model loaded",
            ),
            CapturedLine(
                modelId = "embedded-llama",
                stream = EmbeddedLlamaProcessOutputStream.STDERR,
                line = "warning: slow startup",
            ),
        )
    }

    private data class CapturedLine(
        val modelId: String,
        val stream: EmbeddedLlamaProcessOutputStream,
        val line: String,
    )

    private class FakeProcess(
        stdout: String,
        stderr: String,
    ) : Process() {
        private val stdout = ByteArrayInputStream(stdout.toByteArray())
        private val stderr = ByteArrayInputStream(stderr.toByteArray())

        override fun getOutputStream(): OutputStream =
            ByteArrayOutputStream()

        override fun getInputStream(): InputStream =
            stdout

        override fun getErrorStream(): InputStream =
            stderr

        override fun waitFor(): Int =
            0

        override fun exitValue(): Int =
            0

        override fun destroy() = Unit
    }
}
