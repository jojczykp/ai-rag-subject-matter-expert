package org.alterbit.aisme.chat.embedded

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelContextChunk
import org.alterbit.aisme.chatmodel.ChatModelMode
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.alterbit.aisme.chatmodel.chatModel
import org.junit.jupiter.api.Test

class LlamaRuntimeAiModelClientTest {
    @Test
    fun `exposes configured model id`() {
        val client = LlamaRuntimeAiModelClient(
            model = embeddedModel(id = "llama-runtime-example"),
            runtimeModel = runtimeModel(id = "llama-runtime-example"),
            chatApi = FakeLlamaServerChatApi(),
        )

        client.modelId shouldBe "llama-runtime-example"
    }

    @Test
    fun `sends chat request to configured llama runtime model`() {
        val chatApi = FakeLlamaServerChatApi(answer = " Use two parts water. ")
        val client = LlamaRuntimeAiModelClient(
            model = embeddedModel(id = "llama-runtime-example"),
            runtimeModel = runtimeModel(id = "llama-runtime-example"),
            chatApi = chatApi,
        )

        val response = client.chat(
            AiModelChatRequest(
                modelId = "llama-runtime-example",
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

        response.modelId shouldBe "llama-runtime-example"
        response.answer shouldBe "Use two parts water."
        chatApi.requests.single().model shouldBe "llama-runtime-example"
        chatApi.requests.single().stream shouldBe false
        chatApi.requests.single().messages.single().role shouldBe "user"
        chatApi.requests.single().messages.single().content shouldBe """
            Context:
            Use two parts water for one part rice.

            Question:
            How should I cook rice?
        """.trimIndent()
    }

    @Test
    fun `rejects request for another model id`() {
        val client = LlamaRuntimeAiModelClient(
            model = embeddedModel(id = "llama-runtime-example"),
            runtimeModel = runtimeModel(id = "llama-runtime-example"),
            chatApi = FakeLlamaServerChatApi(),
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.chat(request(modelId = "other-embedded-model"))
        }

        exception.message shouldContain "cannot handle"
    }

    @Test
    fun `rejects blank provider response`() {
        val client = LlamaRuntimeAiModelClient(
            model = embeddedModel(id = "llama-runtime-example"),
            runtimeModel = runtimeModel(id = "llama-runtime-example"),
            chatApi = FakeLlamaServerChatApi(answer = " "),
        )

        val exception = shouldThrow<IllegalStateException> {
            client.chat(request())
        }

        exception.message shouldContain "blank answer"
    }

    private fun request(modelId: String = "llama-runtime-example"): AiModelChatRequest =
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

    private fun runtimeModel(id: String): LlamaRuntimeModelProperties =
        LlamaRuntimeModelProperties(
            id = id,
            displayName = "Embedded Llama",
            ggufFile = "llama.gguf",
            contextSize = 4096,
            license = "Apache-2.0",
            hardwareRequirements = "8 GB RAM",
        )

    private class FakeLlamaServerChatApi(
        private val answer: String = "Fake embedded answer",
    ) : LlamaServerChatApi {
        val requests = mutableListOf<LlamaServerChatRequest>()

        override fun chat(request: LlamaServerChatRequest): LlamaServerChatResponse {
            requests += request
            return LlamaServerChatResponse(
                choices = listOf(
                    LlamaServerChatChoice(
                        message = LlamaServerChatMessage(
                            role = "assistant",
                            content = answer,
                        ),
                    ),
                ),
            )
        }
    }
}
