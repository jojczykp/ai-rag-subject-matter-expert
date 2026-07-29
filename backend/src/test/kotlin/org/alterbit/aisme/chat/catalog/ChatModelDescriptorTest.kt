package org.alterbit.aisme.chat.catalog

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ChatModelDescriptorTest {
    @Test
    fun `creates model descriptor`() {
        val descriptor = ChatModelDescriptor(
            id = "local-ollama-llama",
            enabled = true,
            displayName = "Local Ollama Llama",
            description = "Local Ollama model for chat requests.",
            runtime = ChatModelRuntime.OLLAMA,
            mode = ChatModelMode.LOCAL_SERVER,
            availableOffline = false,
            availability = ChatModelAvailability.CONFIGURED,
            baseUrl = "http://localhost:11434",
            modelName = "llama3.2",
            apiKey = null,
        )

        descriptor.id shouldBe "local-ollama-llama"
        descriptor.enabled shouldBe true
        descriptor.displayName shouldBe "Local Ollama Llama"
        descriptor.description shouldBe "Local Ollama model for chat requests."
        descriptor.runtime shouldBe ChatModelRuntime.OLLAMA
        descriptor.mode shouldBe ChatModelMode.LOCAL_SERVER
        descriptor.availableOffline shouldBe false
        descriptor.availability shouldBe ChatModelAvailability.CONFIGURED
        descriptor.baseUrl shouldBe "http://localhost:11434"
        descriptor.modelName shouldBe "llama3.2"
        descriptor.apiKey shouldBe null
    }

    @Test
    fun `rejects blank id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            descriptor(id = " ")
        }

        exception.message shouldContain "id"
    }

    @Test
    fun `rejects blank display name`() {
        val exception = shouldThrow<IllegalArgumentException> {
            descriptor(displayName = " ")
        }

        exception.message shouldContain "display name"
    }

    @Test
    fun `rejects blank description when configured`() {
        val exception = shouldThrow<IllegalArgumentException> {
            descriptor(description = " ")
        }

        exception.message shouldContain "description"
    }

    @Test
    fun `rejects blank base url when configured`() {
        val exception = shouldThrow<IllegalArgumentException> {
            descriptor(baseUrl = " ")
        }

        exception.message shouldContain "baseUrl"
    }

    @Test
    fun `rejects blank model name when configured`() {
        val exception = shouldThrow<IllegalArgumentException> {
            descriptor(modelName = " ")
        }

        exception.message shouldContain "modelName"
    }

    @Test
    fun `rejects blank api key when configured`() {
        val exception = shouldThrow<IllegalArgumentException> {
            descriptor(apiKey = " ")
        }

        exception.message shouldContain "apiKey"
    }

    private fun descriptor(
        id: String = "local-ollama-llama",
        displayName: String = "Local Ollama Llama",
        description: String? = "Local Ollama model for chat requests.",
        baseUrl: String? = "http://localhost:11434",
        modelName: String? = "llama3.2",
        apiKey: String? = null,
    ): ChatModelDescriptor =
        ChatModelDescriptor(
            id = id,
            enabled = true,
            displayName = displayName,
            description = description,
            runtime = ChatModelRuntime.OLLAMA,
            mode = ChatModelMode.LOCAL_SERVER,
            availableOffline = false,
            availability = ChatModelAvailability.CONFIGURED,
            baseUrl = baseUrl,
            modelName = modelName,
            apiKey = apiKey,
        )
}
