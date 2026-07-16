package org.alterbit.aisme.chat.runtime.huggingface

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chat.ChatModelRequest
import org.alterbit.aisme.chat.ChatModelContextChunk
import org.alterbit.aisme.chat.catalog.ChatModelMode
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.alterbit.aisme.chat.catalog.chatModel
import org.junit.jupiter.api.Test

class HuggingFaceTgiChatModelClientTest {
    @Test
    fun `exposes configured model id`() {
        val client = HuggingFaceTgiChatModelClient(
            model = huggingFaceModel(id = "hf-mistral"),
            chatApi = FakeHuggingFaceTgiChatApi(),
        )

        client.modelId shouldBe "hf-mistral"
    }

    @Test
    fun `sends prompt to configured Hugging Face TGI endpoint`() {
        val chatApi = FakeHuggingFaceTgiChatApi(answer = " Use two parts water. ")
        val client = HuggingFaceTgiChatModelClient(
            model = huggingFaceModel(id = "hf-mistral"),
            chatApi = chatApi,
        )

        val response = client.chat(
            ChatModelRequest(
                modelId = "hf-mistral",
                message = "How should I cook rice?",
                contextChunks = listOf(
                    ChatModelContextChunk(
                        content = "Use two parts water for one part rice.",
                        resourcePath = "subject_documents/culinary_expert/rice.txt",
                        chunkIndex = 0,
                    ),
                ),
                apiTimeout = Duration.ofSeconds(45),
            ),
        )

        response.modelId shouldBe "hf-mistral"
        response.answer shouldBe "Use two parts water."
        chatApi.requests.single().inputs shouldBe """
            Context:
            Use two parts water for one part rice.

            Question:
            How should I cook rice?
        """.trimIndent()
    }

    @Test
    fun `rejects request for another model id`() {
        val client = HuggingFaceTgiChatModelClient(
            model = huggingFaceModel(id = "hf-mistral"),
            chatApi = FakeHuggingFaceTgiChatApi(),
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.chat(request(modelId = "other-model"))
        }

        exception.message shouldContain "cannot handle"
    }

    @Test
    fun `rejects blank provider response`() {
        val client = HuggingFaceTgiChatModelClient(
            model = huggingFaceModel(),
            chatApi = FakeHuggingFaceTgiChatApi(answer = " "),
        )

        val exception = shouldThrow<IllegalStateException> {
            client.chat(request())
        }

        exception.message shouldContain "blank answer"
    }

    private fun request(modelId: String = "hf-mistral"): ChatModelRequest =
        ChatModelRequest(
            modelId = modelId,
            message = "How should I cook rice?",
            contextChunks = emptyList(),
            apiTimeout = Duration.ofSeconds(60),
        )

    private fun huggingFaceModel(id: String = "hf-mistral") =
        chatModel(
            id = id,
            displayName = "Hugging Face Mistral",
            runtime = ChatModelRuntime.HUGGING_FACE_TGI,
            mode = ChatModelMode.ONLINE,
            availableOffline = false,
            baseUrl = "https://hf.example.com",
            modelName = null,
            apiKey = "test-api-key",
        )

    private class FakeHuggingFaceTgiChatApi(
        private val answer: String = "Fake Hugging Face answer",
    ) : HuggingFaceTgiChatApi {
        val requests = mutableListOf<HuggingFaceTgiGenerateRequest>()

        override fun generate(request: HuggingFaceTgiGenerateRequest): HuggingFaceTgiGenerateResponse {
            requests += request
            return HuggingFaceTgiGenerateResponse(generatedText = answer)
        }
    }
}
