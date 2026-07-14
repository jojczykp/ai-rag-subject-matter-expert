package org.alterbit.aisme.chat.ollama

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import org.alterbit.aisme.modelcatalog.ChatModelAvailability
import org.alterbit.aisme.modelcatalog.ChatModelMode
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.chatModel
import org.junit.jupiter.api.Test
import org.springframework.ai.ollama.api.OllamaApi

class OllamaModelAvailabilityCheckerTest {
    @Test
    fun `supports ollama models only`() {
        val checker = OllamaModelAvailabilityChecker(FakeOllamaChatApiFactory())

        checker.supports(chatModel(runtime = ChatModelRuntime.OLLAMA)) shouldBe true
        checker.supports(chatModel(runtime = ChatModelRuntime.SPRING_AI)) shouldBe false
    }

    @Test
    fun `returns available when configured model is listed by ollama`() {
        val factory = FakeOllamaChatApiFactory(modelNames = setOf("llama3.2"))
        val checker = OllamaModelAvailabilityChecker(factory)

        val availability = checker.check(
            model = chatModel(modelName = "llama3.2"),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe ChatModelAvailability.AVAILABLE
        factory.createdClients shouldContainExactly listOf(
            CreatedOllamaClient(
                baseUrl = "http://localhost:11434",
                apiTimeout = Duration.ofSeconds(3),
            ),
        )
    }

    @Test
    fun `returns available when ollama lists latest tag for untagged configured model`() {
        val checker = OllamaModelAvailabilityChecker(
            FakeOllamaChatApiFactory(modelNames = setOf("llama3.2:latest")),
        )

        val availability = checker.check(
            model = chatModel(modelName = "llama3.2"),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe ChatModelAvailability.AVAILABLE
    }

    @Test
    fun `returns unavailable when configured model is not listed by ollama`() {
        val checker = OllamaModelAvailabilityChecker(
            FakeOllamaChatApiFactory(modelNames = setOf("qwen2.5")),
        )

        val availability = checker.check(
            model = chatModel(modelName = "llama3.2"),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe ChatModelAvailability.UNAVAILABLE
    }

    @Test
    fun `returns misconfigured when base url is missing`() {
        val checker = OllamaModelAvailabilityChecker(FakeOllamaChatApiFactory())

        val availability = checker.check(
            model = chatModel(baseUrl = null),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `returns misconfigured when model name is missing`() {
        val checker = OllamaModelAvailabilityChecker(FakeOllamaChatApiFactory())

        val availability = checker.check(
            model = chatModel(modelName = null),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe ChatModelAvailability.MISCONFIGURED
    }

    @Test
    fun `returns unavailable when ollama api fails`() {
        val checker = OllamaModelAvailabilityChecker(FailingOllamaChatApiFactory())

        val availability = checker.check(
            model = chatModel(modelName = "llama3.2"),
            apiTimeout = Duration.ofSeconds(3),
        )

        availability shouldBe ChatModelAvailability.UNAVAILABLE
    }

    private class FakeOllamaChatApiFactory(
        private val modelNames: Set<String> = emptySet(),
    ) : OllamaChatApiFactory {
        val createdClients = mutableListOf<CreatedOllamaClient>()

        override fun create(baseUrl: String, apiTimeout: Duration): OllamaChatApi {
            createdClients += CreatedOllamaClient(baseUrl = baseUrl, apiTimeout = apiTimeout)
            return FakeOllamaChatApi(modelNames)
        }
    }

    private class FailingOllamaChatApiFactory : OllamaChatApiFactory {
        override fun create(baseUrl: String, apiTimeout: Duration): OllamaChatApi =
            object : OllamaChatApi {
                override fun chat(request: OllamaApi.ChatRequest): OllamaApi.ChatResponse =
                    throw UnsupportedOperationException("chat is not used by availability checks")

                override fun modelNames(): Set<String> =
                    throw IllegalStateException("Ollama is unavailable")
            }
    }

    private class FakeOllamaChatApi(
        private val modelNames: Set<String>,
    ) : OllamaChatApi {
        override fun chat(request: OllamaApi.ChatRequest): OllamaApi.ChatResponse =
            OllamaApi.ChatResponse(
                request.model(),
                Instant.EPOCH,
                OllamaApi.Message.builder(OllamaApi.Message.Role.ASSISTANT)
                    .content("Fake Ollama answer")
                    .build(),
                "stop",
                true,
                0L,
                0L,
                0,
                0L,
                0,
                0L,
            )

        override fun modelNames(): Set<String> =
            modelNames
    }

    private data class CreatedOllamaClient(
        val baseUrl: String,
        val apiTimeout: Duration,
    )
}
