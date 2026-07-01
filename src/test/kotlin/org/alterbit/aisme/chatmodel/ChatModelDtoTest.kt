package org.alterbit.aisme.chatmodel

import io.kotest.matchers.shouldBe
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
        dto.runtime shouldBe ChatModelRuntime.OLLAMA
        dto.mode shouldBe ChatModelMode.LOCAL_SERVER
        dto.availability shouldBe ChatModelAvailability.CONFIGURED
        dto.availableOffline shouldBe false
        dto.promptsMayLeaveLocalMachine shouldBe false
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
            displayName = "Local Ollama Llama",
            runtime = runtime,
            mode = mode,
            availableOffline = availableOffline,
            availability = availability,
            baseUrl = "http://localhost:11434",
            modelName = "llama3.2",
        )
}
