package org.alterbit.aisme.embedding

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class EmbeddingModelsPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `uses configured default embedding model catalog`() {
        contextRunner.run { context ->
            val properties = context.getBean<EmbeddingModelProperties>()

            properties.id shouldBe "local-bge-small"
            properties.version shouldBe "1.5"
            properties.runtime shouldBe EmbeddingModelRuntime.ONNX
            properties.modelPath shouldBe "./models/bge-small-en-v1.5/model.onnx"
            properties.tokenizerPath shouldBe "./models/bge-small-en-v1.5/tokenizer.json"
            properties.dimensions shouldBe 384
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddingModelsProperties::class)
    private class PropertiesConfiguration {
        @org.springframework.context.annotation.Bean
        fun embeddingModelProperties(properties: EmbeddingModelsProperties): EmbeddingModelProperties =
            properties.activeModel()
    }
}
