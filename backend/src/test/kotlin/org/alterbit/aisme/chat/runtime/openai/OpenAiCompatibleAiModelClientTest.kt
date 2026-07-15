package org.alterbit.aisme.chat.runtime.openai

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelContextChunk
import org.alterbit.aisme.chat.catalog.ChatModelMode
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.alterbit.aisme.chat.catalog.chatModel
import org.junit.jupiter.api.Test

class OpenAiCompatibleAiModelClientTest {
    @Test
    fun `exposes configured model id`() {
        val client = OpenAiCompatibleAiModelClient(
            model = openAiModel(id = "cloud-gpt"),
            chatApi = FakeOpenAiCompatibleChatApi(),
        )

        client.modelId shouldBe "cloud-gpt"
    }

    @Test
    fun `sends chat request to configured OpenAI-compatible model`() {
        val chatApi = FakeOpenAiCompatibleChatApi(answer = " Use two parts water. ")
        val client = OpenAiCompatibleAiModelClient(
            model = openAiModel(id = "cloud-gpt", modelName = "gpt-4.1-mini"),
            chatApi = chatApi,
        )

        val response = client.chat(
            AiModelChatRequest(
                modelId = "cloud-gpt",
                message = "How should I cook rice?",
                contextChunks = listOf(
                    AiModelContextChunk(
                        content = "Use two parts water for one part rice.",
                        resourcePath = "subject-documents/culinary_expert/rice.txt",
                        chunkIndex = 0,
                    ),
                ),
                apiTimeout = Duration.ofSeconds(45),
            ),
        )

        response.modelId shouldBe "cloud-gpt"
        response.answer shouldBe "Use two parts water."
        chatApi.requests.single().model shouldBe "gpt-4.1-mini"
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
        val client = OpenAiCompatibleAiModelClient(
            model = openAiModel(id = "cloud-gpt"),
            chatApi = FakeOpenAiCompatibleChatApi(),
        )

        val exception = shouldThrow<IllegalArgumentException> {
            client.chat(request(modelId = "other-cloud-model"))
        }

        exception.message shouldContain "cannot handle"
    }

    @Test
    fun `rejects configured model without model name`() {
        val exception = shouldThrow<IllegalStateException> {
            OpenAiCompatibleAiModelClient(
                model = openAiModel(modelName = null),
                chatApi = FakeOpenAiCompatibleChatApi(),
            )
        }

        exception.message shouldContain "requires modelName"
    }

    @Test
    fun `rejects blank provider response`() {
        val client = OpenAiCompatibleAiModelClient(
            model = openAiModel(),
            chatApi = FakeOpenAiCompatibleChatApi(answer = " "),
        )

        val exception = shouldThrow<IllegalStateException> {
            client.chat(request())
        }

        exception.message shouldContain "blank answer"
    }

    private fun request(modelId: String = "cloud-gpt"): AiModelChatRequest =
        AiModelChatRequest(
            modelId = modelId,
            message = "How should I cook rice?",
            contextChunks = emptyList(),
            apiTimeout = Duration.ofSeconds(60),
        )

    private fun openAiModel(
        id: String = "cloud-gpt",
        modelName: String? = "gpt-4.1-mini",
    ) = chatModel(
        id = id,
        displayName = "Cloud GPT",
        runtime = ChatModelRuntime.OPENAI_COMPATIBLE,
        mode = ChatModelMode.ONLINE,
        availableOffline = false,
        baseUrl = "https://api.example.com/v1",
        modelName = modelName,
        apiKey = "test-api-key",
    )

    private class FakeOpenAiCompatibleChatApi(
        private val answer: String = "Fake cloud answer",
    ) : OpenAiCompatibleChatApi {
        val requests = mutableListOf<OpenAiCompatibleChatRequest>()

        override fun chat(request: OpenAiCompatibleChatRequest): OpenAiCompatibleChatResponse {
            requests += request
            return OpenAiCompatibleChatResponse(
                choices = listOf(
                    OpenAiCompatibleChatChoice(
                        message = OpenAiCompatibleChatMessage(
                            role = "assistant",
                            content = answer,
                        ),
                    ),
                ),
            )
        }
    }
}
