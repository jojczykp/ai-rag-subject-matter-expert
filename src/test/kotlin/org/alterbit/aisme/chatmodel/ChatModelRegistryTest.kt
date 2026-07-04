package org.alterbit.aisme.chatmodel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ChatModelRegistryTest {
    @Test
    fun `lists configured models in configuration order`() {
        val registry = ChatModelRegistry(
            ConfiguredChatModelsProperties(
                chatModels = listOf(
                    configuredModel(id = "first-model"),
                    configuredModel(id = "second-model"),
                ),
            ),
        )

        val chatModels = registry.chatModels()

        chatModels shouldHaveSize 2
        chatModels[0].id shouldBe "first-model"
        chatModels[1].id shouldBe "second-model"
        chatModels[0].availability shouldBe ChatModelAvailability.CONFIGURED
        chatModels[1].availability shouldBe ChatModelAvailability.CONFIGURED
    }

    @Test
    fun `finds configured model by id`() {
        val registry = ChatModelRegistry(
            ConfiguredChatModelsProperties(
                chatModels = listOf(
                    configuredModel(id = "local-ollama-llama"),
                ),
            ),
        )

        val model = registry.findById("local-ollama-llama")

        model?.id shouldBe "local-ollama-llama"
        model?.displayName shouldBe "Local Ollama Llama"
        model?.runtime shouldBe ChatModelRuntime.OLLAMA
        model?.mode shouldBe ChatModelMode.LOCAL_SERVER
        model?.availableOffline shouldBe false
        model?.availability shouldBe ChatModelAvailability.CONFIGURED
        model?.baseUrl shouldBe "http://localhost:11434"
        model?.modelName shouldBe "llama3.2"
    }

    @Test
    fun `returns null when configured model is not found`() {
        val registry = ChatModelRegistry(ConfiguredChatModelsProperties())

        registry.findById("missing-model") shouldBe null
    }

    @Test
    fun `gets configured model by id or throws`() {
        val registry = ChatModelRegistry(ConfiguredChatModelsProperties())

        val model = registry.getByIdOrThrow("local-ollama-llama")

        model.id shouldBe "local-ollama-llama"
    }

    @Test
    fun `throws when getting missing configured model by id`() {
        val registry = ChatModelRegistry(ConfiguredChatModelsProperties())

        val exception = shouldThrow<ChatModelNotFoundException> {
            registry.getByIdOrThrow("missing-model")
        }

        exception.message shouldContain "missing-model"
    }

    @Test
    fun `rejects empty configured model list`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(ConfiguredChatModelsProperties(chatModels = emptyList()))
        }

        exception.message shouldContain "aisme.chat-models"
    }

    @Test
    fun `rejects duplicate configured model ids`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                ConfiguredChatModelsProperties(
                    chatModels = listOf(
                        configuredModel(id = "duplicate-model"),
                        configuredModel(id = "duplicate-model"),
                    ),
                ),
            )
        }

        exception.message shouldContain "duplicate"
    }

    @Test
    fun `rejects ollama model without base url`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                ConfiguredChatModelsProperties(
                    chatModels = listOf(configuredModel(baseUrl = null)),
                ),
            )
        }

        exception.message shouldContain "aisme.chat-models[0].base-url"
        exception.message shouldContain "is required"
        exception.message shouldContain "OLLAMA"
    }

    @Test
    fun `rejects ollama model without model name`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                ConfiguredChatModelsProperties(
                    chatModels = listOf(configuredModel(modelName = null)),
                ),
            )
        }

        exception.message shouldContain "aisme.chat-models[0].model-name"
        exception.message shouldContain "is required"
        exception.message shouldContain "OLLAMA"
    }

    @Test
    fun `rejects OpenAI-compatible model without api key`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                ConfiguredChatModelsProperties(
                    chatModels = listOf(
                        configuredModel(
                            runtime = ChatModelRuntime.OPENAI_COMPATIBLE,
                            mode = ChatModelMode.ONLINE,
                            baseUrl = "https://api.example.com/v1",
                            modelName = "gpt-4.1-mini",
                            apiKey = null,
                        ),
                    ),
                ),
            )
        }

        exception.message shouldContain "aisme.chat-models[0].api-key"
        exception.message shouldContain "is required"
        exception.message shouldContain "OPENAI_COMPATIBLE"
    }

    @Test
    fun `rejects Hugging Face endpoint model without base url`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                ConfiguredChatModelsProperties(
                    chatModels = listOf(
                        configuredModel(
                            runtime = ChatModelRuntime.HUGGING_FACE_ENDPOINT,
                            mode = ChatModelMode.ONLINE,
                            baseUrl = null,
                            modelName = null,
                            apiKey = "test-api-key",
                        ),
                    ),
                ),
            )
        }

        exception.message shouldContain "aisme.chat-models[0].base-url"
        exception.message shouldContain "is required"
        exception.message shouldContain "HUGGING_FACE_ENDPOINT"
    }

    @Test
    fun `allows Hugging Face endpoint model without api key`() {
        val registry = ChatModelRegistry(
            ConfiguredChatModelsProperties(
                chatModels = listOf(
                    configuredModel(
                        id = "local-tgi",
                        runtime = ChatModelRuntime.HUGGING_FACE_ENDPOINT,
                        mode = ChatModelMode.LOCAL_SERVER,
                        baseUrl = "http://localhost:8080",
                        modelName = null,
                        apiKey = null,
                    ),
                ),
            ),
        )

        val model = registry.getByIdOrThrow("local-tgi")

        model.apiKey shouldBe null
    }

    private fun configuredModel(
        id: String = "local-ollama-llama",
        runtime: ChatModelRuntime = ChatModelRuntime.OLLAMA,
        mode: ChatModelMode = ChatModelMode.LOCAL_SERVER,
        baseUrl: String? = "http://localhost:11434",
        modelName: String? = "llama3.2",
        apiKey: String? = null,
    ): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            displayName = "Local Ollama Llama",
            runtime = runtime,
            mode = mode,
            availableOffline = false,
            baseUrl = baseUrl,
            modelName = modelName,
            apiKey = apiKey,
        )
}
