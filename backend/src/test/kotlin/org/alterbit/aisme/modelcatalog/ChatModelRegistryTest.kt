package org.alterbit.aisme.modelcatalog

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ChatModelRegistryTest {
    @Test
    fun `lists configured models in display order`() {
        val registry = ChatModelRegistry(
            configuredProperties(
                chatModelsById = mapOf(
                    configuredModel(id = "second-model", displayOrder = 20),
                    configuredModel(id = "first-model", displayOrder = 10),
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
            configuredProperties(
                chatModelsById = mapOf(
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
        val registry = ChatModelRegistry(configuredProperties())

        registry.findById("missing-model") shouldBe null
    }

    @Test
    fun `ignores disabled configured models`() {
        val registry = ChatModelRegistry(
            configuredProperties(
                chatModelsById = mapOf(
                    configuredModel(id = "enabled-model"),
                    configuredModel(id = "disabled-model", enabled = false),
                ),
            ),
        )

        registry.chatModels().map { it.id } shouldBe listOf("enabled-model")
        registry.findById("disabled-model") shouldBe null
    }

    @Test
    fun `gets configured model by id or throws`() {
        val registry = ChatModelRegistry(configuredProperties())

        val model = registry.getByIdOrThrow("local-ollama-llama")

        model.id shouldBe "local-ollama-llama"
    }

    @Test
    fun `throws when getting missing configured model by id`() {
        val registry = ChatModelRegistry(configuredProperties())

        val exception = shouldThrow<ChatModelNotFoundException> {
            registry.getByIdOrThrow("missing-model")
        }

        exception.message shouldContain "missing-model"
    }

    @Test
    fun `rejects empty configured model list`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(configuredProperties(chatModelsById = emptyMap()))
        }

        exception.message shouldContain "aisme.chat.models"
    }

    @Test
    fun `rejects blank configured model id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                configuredProperties(
                    chatModelsById = mapOf(
                        "" to configuredModelValue(),
                    ),
                ),
            )
        }

        exception.message shouldContain "blank ids"
    }

    @Test
    fun `rejects enabled model without config`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                configuredProperties(
                    chatModelsById = mapOf("missing-config" to ChatModelProperties(enabled = true)),
                ),
            )
        }

        exception.message shouldContain "is required"
    }

    @Test
    fun `rejects ollama model without base url`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                configuredProperties(
                    chatRuntimesById = mapOf("local-ollama" to ChatModelRuntimeConfigProperties(type = ChatModelRuntime.OLLAMA)),
                    chatModelsById = mapOf(configuredModel()),
                ),
            )
        }

        exception.message shouldContain "base-url"
        exception.message shouldContain "is required"
        exception.message shouldContain "OLLAMA"
    }

    @Test
    fun `rejects ollama model without model name`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                configuredProperties(
                    chatModelsById = mapOf(configuredModel(modelName = null)),
                ),
            )
        }

        exception.message shouldContain "aisme.chat.models.local-ollama-llama.runtime.model-name"
        exception.message shouldContain "is required"
        exception.message shouldContain "OLLAMA"
    }

    @Test
    fun `allows OpenAI-compatible model without api key`() {
        val registry = ChatModelRegistry(
            configuredProperties(
                chatRuntimesById = mapOf(
                    "openai-compatible-no-key" to ChatModelRuntimeConfigProperties(
                        type = ChatModelRuntime.OPENAI_COMPATIBLE,
                        baseUrl = "https://api.example.com/v1",
                    ),
                ),
                chatModelsById = mapOf(
                    configuredModel(
                        id = "cloud-gpt",
                        runtimeId = "openai-compatible-no-key",
                        modelName = "gpt-4.1-mini",
                    ),
                ),
            ),
        )

        val model = registry.getByIdOrThrow("cloud-gpt")

        model.apiKey shouldBe null
    }

    @Test
    fun `rejects Hugging Face endpoint model without base url`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                configuredProperties(
                    chatRuntimesById = mapOf(
                        "hugging-face-tgi-missing-url" to ChatModelRuntimeConfigProperties(
                            type = ChatModelRuntime.HUGGING_FACE_TGI,
                        ),
                    ),
                    chatModelsById = mapOf(
                        configuredModel(runtimeId = "hugging-face-tgi-missing-url", modelName = null),
                    ),
                ),
            )
        }

        exception.message shouldContain "base-url"
        exception.message shouldContain "is required"
        exception.message shouldContain "HUGGING_FACE_TGI"
    }

    @Test
    fun `allows online Hugging Face endpoint model without api key`() {
        val registry = ChatModelRegistry(
            configuredProperties(
                chatModelsById = mapOf(
                    configuredModel(id = "hf-endpoint", runtimeId = "hugging-face-tgi", modelName = null),
                ),
            ),
        )

        val model = registry.getByIdOrThrow("hf-endpoint")

        model.apiKey shouldBe null
    }

    @Test
    fun `rejects model with unknown runtime id`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                configuredProperties(
                    chatModelsById = mapOf(
                        configuredModel(
                            runtimeId = "missing-runtime",
                        ),
                    ),
                ),
            )
        }

        exception.message shouldContain "runtime.id"
        exception.message shouldContain "missing-runtime"
    }

    @Test
    fun `rejects empty runtime list`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                configuredProperties(
                    chatRuntimesById = emptyMap(),
                    chatModelsById = mapOf(configuredModel()),
                ),
            )
        }

        exception.message shouldContain "aisme.chat.runtimes"
    }

    @Test
    fun `rejects embedded offline model without gguf file`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ChatModelRegistry(
                configuredProperties(
                    chatModelsById = mapOf(
                        configuredModel(
                            runtimeId = "embedded-llama",
                            modelName = "qwen2.5",
                            ggufFile = null,
                        ),
                    ),
                ),
            )
        }

        exception.message shouldContain "gguf-file"
    }

    private fun configuredModel(
        id: String = "local-ollama-llama",
        enabled: Boolean = true,
        displayOrder: Int? = null,
        runtimeId: String? = "local-ollama",
        modelName: String? = "llama3.2",
        ggufFile: String? = "models/qwen.gguf",
    ): Pair<String, ChatModelProperties> =
        id to configuredModelValue(
            enabled = enabled,
            displayOrder = displayOrder,
            displayName = "Local Ollama Llama",
            runtimeId = runtimeId,
            modelName = modelName,
            ggufFile = ggufFile,
            contextSize = 2048,
        )

    private fun configuredModelValue(
        enabled: Boolean = true,
        displayOrder: Int? = null,
        displayName: String? = "Local Ollama Llama",
        runtimeId: String? = "local-ollama",
        modelName: String? = "llama3.2",
        ggufFile: String? = "models/qwen.gguf",
        contextSize: Int? = 2048,
    ): ChatModelProperties =
        ChatModelProperties(
            enabled = enabled,
            displayOrder = displayOrder,
            displayName = displayName,
            runtime = ChatModelRuntimeProperties(
                id = runtimeId,
                modelName = modelName,
                ggufFile = ggufFile,
                contextSize = contextSize,
            ),
        )

    private fun configuredProperties(
        chatRuntimesById: Map<String, ChatModelRuntimeConfigProperties> = defaultRuntimes(),
        chatModelsById: Map<String, ChatModelProperties> = mapOf(configuredModel()),
    ): ChatModelsProperties =
        ChatModelsProperties(
            chatRuntimesById = chatRuntimesById,
            chatModelsById = chatModelsById,
        )

    private fun defaultRuntimes(): Map<String, ChatModelRuntimeConfigProperties> =
        mapOf(
            "embedded-llama" to ChatModelRuntimeConfigProperties(
                type = ChatModelRuntime.EMBEDDED_LLAMA,
                assetDirectory = "./models/llama",
                serverExecutablePath = "./models/llama/bin/llama-server",
            ),
            "local-ollama" to ChatModelRuntimeConfigProperties(
                type = ChatModelRuntime.OLLAMA,
                baseUrl = "http://localhost:11434",
            ),
            "openai-compatible" to ChatModelRuntimeConfigProperties(
                type = ChatModelRuntime.OPENAI_COMPATIBLE,
                baseUrl = "https://api.example.com/v1",
            ),
            "hugging-face-tgi" to ChatModelRuntimeConfigProperties(
                type = ChatModelRuntime.HUGGING_FACE_TGI,
                baseUrl = "https://example.endpoints.huggingface.cloud",
            ),
            "spring-ai" to ChatModelRuntimeConfigProperties(type = ChatModelRuntime.SPRING_AI),
        )
}
