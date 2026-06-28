package org.alterbit.aisme.embedding

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class EmbeddingModelPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `uses default embedding model configuration`() {
        contextRunner.run { context ->
            val properties = context.getBean(EmbeddingModelProperties::class.java)

            properties.id shouldBe "local-bge-small"
            properties.version shouldBe "1.5"
            properties.runtime shouldBe EmbeddingModelRuntime.ONNX
            properties.modelPath shouldBe "./models/bge-small-en-v1.5/model.onnx"
            properties.tokenizerPath shouldBe "./models/bge-small-en-v1.5/tokenizer.json"
            properties.dimensions shouldBe 384
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddingModelProperties::class)
    private class PropertiesConfiguration
}
