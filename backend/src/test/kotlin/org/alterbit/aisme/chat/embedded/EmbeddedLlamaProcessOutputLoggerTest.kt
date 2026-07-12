package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
class EmbeddedLlamaProcessOutputLoggerTest {
    @Test
    fun `captures stdout and stderr lines with model id`() {
        val executor = Executors.newFixedThreadPool(2)
        val capturedLines = CopyOnWriteArrayList<CapturedLine>()
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

    @Test
    fun `maps llama server stderr severity from line content`(output: CapturedOutput) {
        val executor = Executors.newSingleThreadExecutor()
        val outputLogger = EmbeddedLlamaProcessOutputLogger(executor = executor)

        outputLogger.attach(
            modelId = "embedded-llama",
            process = FakeProcess(
                stdout = "",
                stderr = """
                    0.00.001 I srv llama_server: model loaded
                    0.00.002 W load: token warning
                    0.00.003 E srv llama_server: failed
                """.trimIndent(),
            ),
        ).forEach { it.get() }
        outputLogger.stop()

        output.out shouldContain "INFO"
        output.out shouldContain "llama_server: model loaded"
        output.out shouldContain "WARN"
        output.out shouldContain "token warning"
        output.out shouldContain "ERROR"
        output.out shouldContain "llama_server: failed"
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
