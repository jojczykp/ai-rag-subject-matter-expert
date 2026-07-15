package org.alterbit.aisme.embedding.catalog

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Test

class EmbeddingModelAvailabilityServiceTest {
    @Test
    fun `marks disabled models as unavailable without checking runtime`() {
        val checker = RecordingAvailabilityChecker(
            supportedRuntime = EmbeddingModelRuntime.OLLAMA,
            availability = EmbeddingModelAvailability.AVAILABLE,
        )
        val service = EmbeddingModelAvailabilityService(
            properties = EmbeddingModelAvailabilityProperties(timeout = Duration.ofSeconds(5)),
            checkers = listOf(checker),
            clock = Clock.systemUTC(),
        )

        val model = service.withAvailability(embeddingModel(enabled = false))

        model.availability shouldBe EmbeddingModelAvailability.UNAVAILABLE
        checker.checkedModels shouldContainExactly emptyList()
    }

    @Test
    fun `keeps configured availability when no checker supports model`() {
        val service = EmbeddingModelAvailabilityService(
            properties = EmbeddingModelAvailabilityProperties(timeout = Duration.ofSeconds(5)),
            checkers = emptyList(),
            clock = Clock.systemUTC(),
        )

        val model = service.withAvailability(embeddingModel())

        model.availability shouldBe EmbeddingModelAvailability.CONFIGURED
    }

    @Test
    fun `uses supported checker to resolve availability`() {
        val checker = RecordingAvailabilityChecker(
            supportedRuntime = EmbeddingModelRuntime.OLLAMA,
            availability = EmbeddingModelAvailability.AVAILABLE,
        )
        val service = EmbeddingModelAvailabilityService(
            properties = EmbeddingModelAvailabilityProperties(timeout = Duration.ofSeconds(2)),
            checkers = listOf(checker),
            clock = Clock.systemUTC(),
        )

        val model = service.withAvailability(embeddingModel(runtime = EmbeddingModelRuntime.OLLAMA))

        model.availability shouldBe EmbeddingModelAvailability.AVAILABLE
        checker.checkedModels shouldContainExactly listOf("ollama-nomic-embed")
        checker.checkedTimeouts shouldContainExactly listOf(Duration.ofSeconds(2))
    }

    @Test
    fun `uses cached availability within cache ttl`() {
        val clock = MutableClock(Instant.parse("2026-06-30T10:00:00Z"))
        val checker = RecordingAvailabilityChecker(
            supportedRuntime = EmbeddingModelRuntime.OLLAMA,
            availability = EmbeddingModelAvailability.AVAILABLE,
        )
        val service = EmbeddingModelAvailabilityService(
            properties = EmbeddingModelAvailabilityProperties(
                timeout = Duration.ofSeconds(2),
                cacheTtl = Duration.ofSeconds(5),
            ),
            checkers = listOf(checker),
            clock = clock,
        )

        service.withAvailability(embeddingModel(runtime = EmbeddingModelRuntime.OLLAMA))
        clock.advanceBy(Duration.ofSeconds(4))
        val model = service.withAvailability(embeddingModel(runtime = EmbeddingModelRuntime.OLLAMA))

        model.availability shouldBe EmbeddingModelAvailability.AVAILABLE
        checker.checkedModels shouldContainExactly listOf("ollama-nomic-embed")
    }

    private class RecordingAvailabilityChecker(
        private val supportedRuntime: EmbeddingModelRuntime,
        private val availability: EmbeddingModelAvailability,
    ) : EmbeddingModelAvailabilityChecker {
        val checkedModels = mutableListOf<String>()
        val checkedTimeouts = mutableListOf<Duration>()

        override fun supports(model: EmbeddingModelDescriptor): Boolean =
            model.runtime == supportedRuntime

        override fun check(model: EmbeddingModelDescriptor, apiTimeout: Duration): EmbeddingModelAvailability {
            checkedModels += model.id
            checkedTimeouts += apiTimeout
            return availability
        }
    }

    private class MutableClock(
        private var currentInstant: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId =
            ZoneId.of("UTC")

        override fun withZone(zone: ZoneId): Clock =
            this

        override fun instant(): Instant =
            currentInstant

        fun advanceBy(duration: Duration) {
            currentInstant = currentInstant.plus(duration)
        }
    }
}

fun embeddingModel(
    enabled: Boolean = true,
    runtime: EmbeddingModelRuntime = EmbeddingModelRuntime.OLLAMA,
): EmbeddingModelDescriptor =
    EmbeddingModelDescriptor(
        id = "ollama-nomic-embed",
        enabled = enabled,
        displayOrder = 10,
        displayName = "Ollama Nomic Embed",
        runtime = runtime,
        mode = runtime.mode,
        availability = EmbeddingModelAvailability.CONFIGURED,
        version = "v1.5",
        dimensions = 768,
        baseUrl = "http://localhost:11434",
        modelName = "nomic-embed-text:v1.5",
        modelPath = "./models/model.onnx",
        tokenizerPath = "./models/tokenizer.json",
    )

val EmbeddingModelRuntime.mode: EmbeddingModelMode
    get() = when (this) {
        EmbeddingModelRuntime.ONNX -> EmbeddingModelMode.EMBEDDED_OFFLINE
        EmbeddingModelRuntime.OLLAMA -> EmbeddingModelMode.LOCAL_SERVER
    }
