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
    }

    @Test
    fun `returns null when configured model is not found`() {
        val registry = ChatModelRegistry(ConfiguredChatModelsProperties())

        registry.findById("missing-model") shouldBe null
    }

    @Test
    fun `requires configured model by id`() {
        val registry = ChatModelRegistry(ConfiguredChatModelsProperties())

        val model = registry.requireById("local-ollama-llama")

        model.id shouldBe "local-ollama-llama"
    }

    @Test
    fun `throws when required model is not found`() {
        val registry = ChatModelRegistry(ConfiguredChatModelsProperties())

        val exception = shouldThrow<ChatModelNotFoundException> {
            registry.requireById("missing-model")
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

    private fun configuredModel(id: String): ConfiguredChatModelProperties =
        ConfiguredChatModelProperties(
            id = id,
            displayName = "Local Ollama Llama",
            runtime = ChatModelRuntime.OLLAMA,
            mode = ChatModelMode.LOCAL_SERVER,
            availableOffline = false,
            baseUrl = "http://localhost:11434",
        )
}
