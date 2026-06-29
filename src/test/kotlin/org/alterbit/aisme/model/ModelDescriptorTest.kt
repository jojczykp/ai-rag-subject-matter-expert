package org.alterbit.aisme.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ModelDescriptorTest {
    @Test
    fun `creates model descriptor`() {
        val descriptor = ModelDescriptor(
            id = "local-ollama-llama",
            displayName = "Local Ollama Llama",
            runtime = ModelRuntime.OLLAMA,
            mode = ModelMode.LOCAL_SERVER,
            availableOffline = false,
            availability = ModelAvailability.CONFIGURED,
            baseUrl = "http://localhost:11434",
        )

        descriptor.id shouldBe "local-ollama-llama"
        descriptor.displayName shouldBe "Local Ollama Llama"
        descriptor.runtime shouldBe ModelRuntime.OLLAMA
        descriptor.mode shouldBe ModelMode.LOCAL_SERVER
        descriptor.availableOffline shouldBe false
        descriptor.availability shouldBe ModelAvailability.CONFIGURED
        descriptor.baseUrl shouldBe "http://localhost:11434"
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
    fun `rejects blank base url when configured`() {
        val exception = shouldThrow<IllegalArgumentException> {
            descriptor(baseUrl = " ")
        }

        exception.message shouldContain "baseUrl"
    }

    private fun descriptor(
        id: String = "local-ollama-llama",
        displayName: String = "Local Ollama Llama",
        baseUrl: String? = "http://localhost:11434",
    ): ModelDescriptor =
        ModelDescriptor(
            id = id,
            displayName = displayName,
            runtime = ModelRuntime.OLLAMA,
            mode = ModelMode.LOCAL_SERVER,
            availableOffline = false,
            availability = ModelAvailability.CONFIGURED,
            baseUrl = baseUrl,
        )
}
