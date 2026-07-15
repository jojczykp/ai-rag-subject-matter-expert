package org.alterbit.aisme.embedding.runtime.onnx

import java.nio.file.Files
import java.time.Duration
import kotlin.io.path.Path
import org.alterbit.aisme.embedding.catalog.EmbeddingModelAvailability
import org.alterbit.aisme.embedding.catalog.EmbeddingModelAvailabilityChecker
import org.alterbit.aisme.embedding.catalog.EmbeddingModelDescriptor
import org.alterbit.aisme.embedding.catalog.EmbeddingModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OnnxEmbeddingModelAvailabilityChecker : EmbeddingModelAvailabilityChecker {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(model: EmbeddingModelDescriptor): Boolean =
        model.runtime == EmbeddingModelRuntime.ONNX

    override fun check(model: EmbeddingModelDescriptor, apiTimeout: Duration): EmbeddingModelAvailability {
        val modelPath = model.modelPath ?: return EmbeddingModelAvailability.MISCONFIGURED.also {
            logger.warn("ONNX embedding model '{}' is missing model path", model.id)
        }
        val tokenizerPath = model.tokenizerPath ?: return EmbeddingModelAvailability.MISCONFIGURED.also {
            logger.warn("ONNX embedding model '{}' is missing tokenizer path", model.id)
        }

        if (!isReadableRegularFile(modelPath)) {
            logger.warn("ONNX embedding model '{}' model file is not readable: {}", model.id, modelPath)
            return EmbeddingModelAvailability.UNAVAILABLE
        }
        if (!isReadableRegularFile(tokenizerPath)) {
            logger.warn("ONNX embedding model '{}' tokenizer file is not readable: {}", model.id, tokenizerPath)
            return EmbeddingModelAvailability.UNAVAILABLE
        }

        logger.info("ONNX embedding model '{}' assets are available", model.id)
        return EmbeddingModelAvailability.AVAILABLE
    }

    private fun isReadableRegularFile(configuredPath: String): Boolean {
        val path = Path(configuredPath)
        return Files.isRegularFile(path) && Files.isReadable(path)
    }
}
