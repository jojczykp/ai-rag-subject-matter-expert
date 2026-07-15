package org.alterbit.aisme.embedding.runtime.onnx

import io.kotest.matchers.shouldBe
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.createFile
import org.alterbit.aisme.embedding.catalog.EmbeddingModelAvailability
import org.alterbit.aisme.embedding.catalog.EmbeddingModelRuntime
import org.alterbit.aisme.embedding.catalog.embeddingModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class OnnxEmbeddingModelAvailabilityCheckerTest {
    private val checker = OnnxEmbeddingModelAvailabilityChecker()

    @Test
    fun `marks onnx model available when model and tokenizer files are readable`(
        @TempDir tempDir: Path,
    ) {
        val modelPath = tempDir.resolve("model.onnx").createFile()
        val tokenizerPath = tempDir.resolve("tokenizer.json").createFile()

        val availability = checker.check(
            model = embeddingModel(runtime = EmbeddingModelRuntime.ONNX).copy(
                modelPath = modelPath.toString(),
                tokenizerPath = tokenizerPath.toString(),
            ),
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe EmbeddingModelAvailability.AVAILABLE
    }

    @Test
    fun `marks onnx model unavailable when model file is missing`(
        @TempDir tempDir: Path,
    ) {
        val tokenizerPath = tempDir.resolve("tokenizer.json").createFile()

        val availability = checker.check(
            model = embeddingModel(runtime = EmbeddingModelRuntime.ONNX).copy(
                modelPath = tempDir.resolve("missing.onnx").toString(),
                tokenizerPath = tokenizerPath.toString(),
            ),
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe EmbeddingModelAvailability.UNAVAILABLE
    }

    @Test
    fun `marks onnx model misconfigured when tokenizer path is missing`() {
        val availability = checker.check(
            model = embeddingModel(runtime = EmbeddingModelRuntime.ONNX).copy(tokenizerPath = null),
            apiTimeout = Duration.ofSeconds(5),
        )

        availability shouldBe EmbeddingModelAvailability.MISCONFIGURED
    }
}
