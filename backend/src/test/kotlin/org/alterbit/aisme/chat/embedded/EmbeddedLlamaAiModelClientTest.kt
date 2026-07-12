package org.alterbit.aisme.chat.embedded

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelContextChunk
import org.alterbit.aisme.modelcatalog.ChatModelMode
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.alterbit.aisme.modelcatalog.chatModel
import org.junit.jupiter.api.Test

class EmbeddedLlamaAiModelClientTest {
    @Test
    fun `exposes configured model id`() {
        val client = EmbeddedLlamaAiModelClient(
            model = embeddedModel(id = "embedded-llama-tiny"),
            runtimeModel = runtimeModel(id = "embedded-llama-tiny"),
            chatApi = FakeLlamaServerChatApi(),
        )

        client.modelId shouldBe "embedded-llama-tiny"
    }

    @Test
    fun `sends chat request to configured embedded llama model`() {
        val chatApi = FakeLlamaServerChatApi(answer = " Use two parts water. ")
        val client = EmbeddedLlamaAiModelClient(
            model = embeddedModel(id = "embedded-llama-tiny"),
            runtimeModel = runtimeModel(id = "embedded-llama-tiny"),
            chatApi = chatApi,
        )

        val response = client.chat(
            AiModelChatRequest(
                modelId = "embedded-llama-tiny",
                message = "How should I cook rice?",
                contextChunks = listOf(
                    AiModelContextChunk(
                        content = "Use two parts water for one part rice.",
                        resourcePath = "subject-documents/culinary_expert/rice.txt",
                        chunkIndex = 0,
                    ),
                ),
                timeout = Duration.ofSeconds(45),
            ),
        )

        response.modelId shouldBe "embedded-llama-tiny"
        response.answer shouldBe "Use two parts water."
        chatApi.requests.single().stream shouldBe false
        chatApi.requests.single().prompt shouldBe """
            Context:
            Use two parts water for one part rice.

            Question:
            How should I cook rice?
        """.trimIndent()
    }

    @Test
    fun `rejects request for another model id`() {
        val client = EmbeddedLlamaAiModelClient(
            model = embeddedModel(id = "embedded-llama-tiny"),
            runtimeModel = runtimeModel(id = "embedded-llama-tiny"),
            chatApi = FakeLlamaServerChatApi(),
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.chat(request(modelId = "other-embedded-model"))
        }

        exception.message shouldContain "cannot handle"
    }

    @Test
    fun `rejects blank provider response`() {
        val client = EmbeddedLlamaAiModelClient(
            model = embeddedModel(id = "embedded-llama-tiny"),
            runtimeModel = runtimeModel(id = "embedded-llama-tiny"),
            chatApi = FakeLlamaServerChatApi(answer = " "),
        )

        val exception = shouldThrow<IllegalStateException> {
            client.chat(request())
        }

        exception.message shouldContain "blank answer"
    }

    private fun request(modelId: String = "embedded-llama-tiny"): AiModelChatRequest =
        AiModelChatRequest(
            modelId = modelId,
            message = "How should I cook rice?",
            contextChunks = emptyList(),
            timeout = Duration.ofSeconds(60),
        )

    private fun embeddedModel(id: String) = chatModel(
        id = id,
        displayName = "Embedded Llama",
        runtime = ChatModelRuntime.EMBEDDED_OFFLINE,
        mode = ChatModelMode.EMBEDDED_OFFLINE,
        availableOffline = true,
        baseUrl = null,
        modelName = null,
    )

    private fun runtimeModel(id: String): EmbeddedLlamaModelProperties =
        EmbeddedLlamaModelProperties(
            id = id,
            displayName = "Embedded Llama",
            ggufFile = "llama.gguf",
            contextSize = 4096,
        )

    private class FakeLlamaServerChatApi(
        private val answer: String = "Fake embedded answer",
    ) : LlamaServerChatApi {
        val requests = mutableListOf<LlamaServerCompletionRequest>()

        override fun complete(request: LlamaServerCompletionRequest): LlamaServerCompletionResponse {
            requests += request
            return LlamaServerCompletionResponse(content = answer)
        }
    }
}
