package org.alterbit.aisme.chat.embedded

import jakarta.annotation.PreDestroy
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LlamaRuntimeProcessOutputLogger(
    private val executor: ExecutorService = createExecutor(),
    private val lineConsumer: ((String, LlamaRuntimeProcessOutputStream, String) -> Unit)? = null,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun attach(modelId: String, process: Process): List<Future<*>> =
        listOf(
            executor.submit { logLines(modelId, LlamaRuntimeProcessOutputStream.STDOUT, process.inputStream) },
            executor.submit { logLines(modelId, LlamaRuntimeProcessOutputStream.STDERR, process.errorStream) },
        )

    @PreDestroy
    fun stop() {
        executor.shutdownNow()
    }

    private fun logLines(
        modelId: String,
        stream: LlamaRuntimeProcessOutputStream,
        input: InputStream,
    ) {
        try {
            input.bufferedReader().useLines { lines ->
                lines.forEach { line -> logLine(modelId, stream, line) }
            }
        } catch (ex: IOException) {
            logger.warn("Stopped reading llama-server {} for model '{}': {}", stream.label, modelId, ex.message)
        }
    }

    private fun logLine(
        modelId: String,
        stream: LlamaRuntimeProcessOutputStream,
        line: String,
    ) {
        lineConsumer?.invoke(modelId, stream, line) ?: when (stream) {
            LlamaRuntimeProcessOutputStream.STDOUT ->
                logger.info("llama-server [{}] stdout: {}", modelId, line)

            LlamaRuntimeProcessOutputStream.STDERR ->
                logger.warn("llama-server [{}] stderr: {}", modelId, line)
        }
    }

    private companion object {
        fun createExecutor(): ExecutorService {
            val counter = AtomicInteger(0)
            return Executors.newCachedThreadPool { runnable ->
                Thread(runnable, "llama-runtime-output-${counter.incrementAndGet()}").apply {
                    isDaemon = true
                }
            }
        }
    }
}

enum class LlamaRuntimeProcessOutputStream(
    val label: String,
) {
    STDOUT("stdout"),
    STDERR("stderr"),
}
