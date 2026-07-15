package org.alterbit.aisme.embedding

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class EmbeddingPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `uses configured default embedding model catalog`() {
        contextRunner.run { context ->
            val properties = context.getBean<EmbeddingProperties>()
            val enabledModels = properties.enabledModels()

            enabledModels shouldHaveSize 2
            enabledModels[0].id shouldBe "local-bge-small"
            enabledModels[0].version shouldBe "1.5"
            enabledModels[0].runtime shouldBe EmbeddingModelRuntime.ONNX
            enabledModels[0].modelPath shouldBe "./models/bge-small-en-v1.5/model.onnx"
            enabledModels[0].tokenizerPath shouldBe "./models/bge-small-en-v1.5/tokenizer.json"
            enabledModels[0].dimensions shouldBe 384
            enabledModels[1].id shouldBe "ollama-nomic-embed"
            enabledModels[1].version shouldBe "v1.5"
            enabledModels[1].runtime shouldBe EmbeddingModelRuntime.OLLAMA
            enabledModels[1].baseUrl shouldBe "http://localhost:11434"
            enabledModels[1].modelName shouldBe "nomic-embed-text:v1.5"
            enabledModels[1].dimensions shouldBe 768
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddingProperties::class)
    private class PropertiesConfiguration
}
