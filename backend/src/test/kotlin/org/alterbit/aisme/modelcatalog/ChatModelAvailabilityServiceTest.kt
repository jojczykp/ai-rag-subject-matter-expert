package org.alterbit.aisme.modelcatalog

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Test

class ChatModelAvailabilityServiceTest {
    @Test
    fun `keeps configured availability when no checker supports model`() {
        val service = ChatModelAvailabilityService(
            properties = ChatModelAvailabilityProperties(timeout = Duration.ofSeconds(5)),
            checkers = emptyList(),
            clock = Clock.systemUTC(),
        )

        val model = service.withAvailability(chatModel())

        model.availability shouldBe ChatModelAvailability.CONFIGURED
    }

    @Test
    fun `uses supported checker to resolve availability`() {
        val checker = RecordingAvailabilityChecker(
            supportedRuntime = ChatModelRuntime.OLLAMA,
            availability = ChatModelAvailability.AVAILABLE,
        )
        val service = ChatModelAvailabilityService(
            properties = ChatModelAvailabilityProperties(timeout = Duration.ofSeconds(2)),
            checkers = listOf(checker),
            clock = Clock.systemUTC(),
        )

        val model = service.withAvailability(chatModel(runtime = ChatModelRuntime.OLLAMA))

        model.availability shouldBe ChatModelAvailability.AVAILABLE
        checker.checkedModels shouldContainExactly listOf("local-ollama-llama")
        checker.checkedTimeouts shouldContainExactly listOf(Duration.ofSeconds(2))
    }

    @Test
    fun `uses cached availability within cache ttl`() {
        val clock = MutableClock(Instant.parse("2026-06-30T10:00:00Z"))
        val checker = RecordingAvailabilityChecker(
            supportedRuntime = ChatModelRuntime.OLLAMA,
            availability = ChatModelAvailability.AVAILABLE,
        )
        val service = ChatModelAvailabilityService(
            properties = ChatModelAvailabilityProperties(
                timeout = Duration.ofSeconds(2),
                cacheTtl = Duration.ofSeconds(5),
            ),
            checkers = listOf(checker),
            clock = clock,
        )

        service.withAvailability(chatModel(runtime = ChatModelRuntime.OLLAMA))
        clock.advanceBy(Duration.ofSeconds(4))
        val model = service.withAvailability(chatModel(runtime = ChatModelRuntime.OLLAMA))

        model.availability shouldBe ChatModelAvailability.AVAILABLE
        checker.checkedModels shouldContainExactly listOf("local-ollama-llama")
    }

    @Test
    fun `refreshes cached availability after cache ttl expires`() {
        val clock = MutableClock(Instant.parse("2026-06-30T10:00:00Z"))
        val checker = RecordingAvailabilityChecker(
            supportedRuntime = ChatModelRuntime.OLLAMA,
            availability = ChatModelAvailability.AVAILABLE,
        )
        val service = ChatModelAvailabilityService(
            properties = ChatModelAvailabilityProperties(
                timeout = Duration.ofSeconds(2),
                cacheTtl = Duration.ofSeconds(5),
            ),
            checkers = listOf(checker),
            clock = clock,
        )

        service.withAvailability(chatModel(runtime = ChatModelRuntime.OLLAMA))
        clock.advanceBy(Duration.ofSeconds(5))
        service.withAvailability(chatModel(runtime = ChatModelRuntime.OLLAMA))

        checker.checkedModels shouldContainExactly listOf(
            "local-ollama-llama",
            "local-ollama-llama",
        )
    }

    @Test
    fun `checks model list in order`() {
        val checker = RecordingAvailabilityChecker(
            supportedRuntime = ChatModelRuntime.OLLAMA,
            availability = ChatModelAvailability.UNAVAILABLE,
        )
        val service = ChatModelAvailabilityService(
            properties = ChatModelAvailabilityProperties(timeout = Duration.ofSeconds(5)),
            checkers = listOf(checker),
            clock = Clock.systemUTC(),
        )

        val models = service.withAvailability(
            listOf(
                chatModel(id = "local-ollama-llama", runtime = ChatModelRuntime.OLLAMA),
                chatModel(id = "cloud-gpt", runtime = ChatModelRuntime.SPRING_AI),
            ),
        )

        models.map { it.id } shouldContainExactly listOf("local-ollama-llama", "cloud-gpt")
        models.map { it.availability } shouldContainExactly listOf(
            ChatModelAvailability.UNAVAILABLE,
            ChatModelAvailability.CONFIGURED,
        )
    }

    private class RecordingAvailabilityChecker(
        private val supportedRuntime: ChatModelRuntime,
        private val availability: ChatModelAvailability,
    ) : ChatModelAvailabilityChecker {
        val checkedModels = mutableListOf<String>()
        val checkedTimeouts = mutableListOf<Duration>()

        override fun supports(model: ChatModelDescriptor): Boolean =
            model.runtime == supportedRuntime

        override fun check(model: ChatModelDescriptor, apiTimeout: Duration): ChatModelAvailability {
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
