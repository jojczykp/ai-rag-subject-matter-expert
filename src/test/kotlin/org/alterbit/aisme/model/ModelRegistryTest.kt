package org.alterbit.aisme.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ModelRegistryTest {
    @Test
    fun `lists configured models in configuration order`() {
        val registry = ModelRegistry(
            ConfiguredModelsProperties(
                models = listOf(
                    configuredModel(id = "first-model"),
                    configuredModel(id = "second-model"),
                ),
            ),
        )

        val models = registry.models()

        models shouldHaveSize 2
        models[0].id shouldBe "first-model"
        models[1].id shouldBe "second-model"
        models[0].availability shouldBe ModelAvailability.CONFIGURED
        models[1].availability shouldBe ModelAvailability.CONFIGURED
    }

    @Test
    fun `finds configured model by id`() {
        val registry = ModelRegistry(
            ConfiguredModelsProperties(
                models = listOf(
                    configuredModel(id = "local-ollama-llama"),
                ),
            ),
        )

        val model = registry.findById("local-ollama-llama")

        model?.id shouldBe "local-ollama-llama"
        model?.displayName shouldBe "Local Ollama Llama"
        model?.runtime shouldBe ModelRuntime.OLLAMA
        model?.mode shouldBe ModelMode.LOCAL_SERVER
        model?.availableOffline shouldBe false
        model?.availability shouldBe ModelAvailability.CONFIGURED
        model?.baseUrl shouldBe "http://localhost:11434"
    }

    @Test
    fun `returns null when configured model is not found`() {
        val registry = ModelRegistry(ConfiguredModelsProperties())

        registry.findById("missing-model") shouldBe null
    }

    @Test
    fun `requires configured model by id`() {
        val registry = ModelRegistry(ConfiguredModelsProperties())

        val model = registry.requireById("local-ollama-llama")

        model.id shouldBe "local-ollama-llama"
    }

    @Test
    fun `throws when required model is not found`() {
        val registry = ModelRegistry(ConfiguredModelsProperties())

        val exception = shouldThrow<ModelNotFoundException> {
            registry.requireById("missing-model")
        }

        exception.message shouldContain "missing-model"
    }

    @Test
    fun `rejects empty configured model list`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ModelRegistry(ConfiguredModelsProperties(models = emptyList()))
        }

        exception.message shouldContain "aisme.models"
    }

    @Test
    fun `rejects duplicate configured model ids`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ModelRegistry(
                ConfiguredModelsProperties(
                    models = listOf(
                        configuredModel(id = "duplicate-model"),
                        configuredModel(id = "duplicate-model"),
                    ),
                ),
            )
        }

        exception.message shouldContain "duplicate"
    }

    private fun configuredModel(id: String): ConfiguredModelProperties =
        ConfiguredModelProperties(
            id = id,
            displayName = "Local Ollama Llama",
            runtime = ModelRuntime.OLLAMA,
            mode = ModelMode.LOCAL_SERVER,
            availableOffline = false,
            baseUrl = "http://localhost:11434",
        )
}
