package org.alterbit.aisme.chatmodel

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test

class ChatModelAvailabilityServiceTest {
    @Test
    fun `keeps configured availability when no checker supports model`() {
        val service = ChatModelAvailabilityService(
            properties = ChatModelAvailabilityProperties(timeout = Duration.ofSeconds(5)),
            checkers = emptyList(),
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
        )

        val model = service.withAvailability(chatModel(runtime = ChatModelRuntime.OLLAMA))

        model.availability shouldBe ChatModelAvailability.AVAILABLE
        checker.checkedModels shouldContainExactly listOf("local-ollama-llama")
        checker.checkedTimeouts shouldContainExactly listOf(Duration.ofSeconds(2))
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

        override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability {
            checkedModels += model.id
            checkedTimeouts += timeout
            return availability
        }
    }
}
