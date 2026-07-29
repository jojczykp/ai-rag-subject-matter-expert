package org.alterbit.aisme.chat.api

import io.kotest.matchers.shouldBe
import org.alterbit.aisme.chat.catalog.ChatModelAvailability
import org.alterbit.aisme.chat.catalog.ChatModelCapability
import org.alterbit.aisme.chat.catalog.ChatModelDescriptor
import org.alterbit.aisme.chat.catalog.ChatModelMode
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeRequirement
import org.junit.jupiter.api.Test

class ChatModelDtoTest {
    @Test
    fun `maps local server chat model descriptor to api dto`() {
        val dto = descriptor(
            mode = ChatModelMode.LOCAL_SERVER,
            availableOffline = false,
        ).toDto()

        dto.id shouldBe "local-ollama-llama"
        dto.displayName shouldBe "Local Ollama Llama"
        dto.description shouldBe "Local Ollama model for chat requests."
        dto.runtime shouldBe ChatModelRuntime.OLLAMA
        dto.mode shouldBe ChatModelMode.LOCAL_SERVER
        dto.availability shouldBe ChatModelAvailability.CONFIGURED
        dto.availableOffline shouldBe false
        dto.promptsMayLeaveLocalMachine shouldBe false
        dto.capabilities shouldBe listOf(ChatModelCapability.CHAT)
        dto.runtimeRequirements shouldBe listOf(ChatModelRuntimeRequirement.REQUIRES_OLLAMA_SERVER)
    }

    @Test
    fun `marks online chat model as potentially sending prompts outside local machine`() {
        val dto = descriptor(
            runtime = ChatModelRuntime.SPRING_AI,
            mode = ChatModelMode.ONLINE,
            availableOffline = false,
        ).toDto()

        dto.promptsMayLeaveLocalMachine shouldBe true
    }

    @Test
    fun `maps OpenAI-compatible runtime requirements`() {
        val dto = descriptor(
            runtime = ChatModelRuntime.OPENAI_COMPATIBLE,
            mode = ChatModelMode.ONLINE,
            availableOffline = false,
        ).toDto()

        dto.runtimeRequirements shouldBe listOf(
            ChatModelRuntimeRequirement.REQUIRES_NETWORK,
            ChatModelRuntimeRequirement.REQUIRES_API_KEY,
        )
    }

    @Test
    fun `maps embedded offline runtime requirements`() {
        val dto = descriptor(
            runtime = ChatModelRuntime.EMBEDDED_LLAMA,
            mode = ChatModelMode.EMBEDDED_OFFLINE,
            availableOffline = true,
        ).toDto()

        dto.runtimeRequirements shouldBe listOf(
            ChatModelRuntimeRequirement.REQUIRES_LOCAL_GGUF_MODEL,
            ChatModelRuntimeRequirement.REQUIRES_LLAMA_SERVER_EXECUTABLE,
        )
    }

    @Test
    fun `maps Hugging Face endpoint runtime requirements`() {
        val dto = descriptor(
            runtime = ChatModelRuntime.HUGGING_FACE_TGI,
            mode = ChatModelMode.ONLINE,
            availableOffline = false,
        ).toDto()

        dto.runtimeRequirements shouldBe listOf(ChatModelRuntimeRequirement.REQUIRES_NETWORK)
    }

    @Test
    fun `maps all supported availability states`() {
        ChatModelAvailability.entries.forEach { availability ->
            val dto = descriptor(
                mode = ChatModelMode.LOCAL_SERVER,
                availableOffline = false,
                availability = availability,
            ).toDto()

            dto.availability shouldBe availability
        }
    }

    private fun descriptor(
        runtime: ChatModelRuntime = ChatModelRuntime.OLLAMA,
        mode: ChatModelMode,
        availableOffline: Boolean,
        availability: ChatModelAvailability = ChatModelAvailability.CONFIGURED,
    ): ChatModelDescriptor =
        ChatModelDescriptor(
            id = "local-ollama-llama",
            enabled = true,
            displayName = "Local Ollama Llama",
            description = "Local Ollama model for chat requests.",
            runtime = runtime,
            mode = mode,
            availableOffline = availableOffline,
            availability = availability,
            baseUrl = "http://localhost:11434",
            modelName = "llama3.2",
            apiKey = null,
        )
}
