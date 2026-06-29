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

    private fun descriptor(
        runtime: ChatModelRuntime = ChatModelRuntime.OLLAMA,
        mode: ChatModelMode,
        availableOffline: Boolean,
    ): ChatModelDescriptor =
        ChatModelDescriptor(
            id = "local-ollama-llama",
            displayName = "Local Ollama Llama",
            runtime = runtime,
            mode = mode,
            availableOffline = availableOffline,
            availability = ChatModelAvailability.CONFIGURED,
            baseUrl = "http://localhost:11434",
        )
}
