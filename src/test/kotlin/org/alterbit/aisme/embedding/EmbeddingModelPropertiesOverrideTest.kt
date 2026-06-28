package org.alterbit.aisme.embedding

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class EmbeddingModelPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.embedding-model.id=custom-embedding",
            "aisme.embedding-model.version=2026-01",
            "aisme.embedding-model.runtime=ONNX",
            "aisme.embedding-model.model-path=/models/custom/model.onnx",
            "aisme.embedding-model.tokenizer-path=/models/custom/tokenizer.json",
            "aisme.embedding-model.dimensions=768",
        )

    @Test
    fun `uses configured embedding model properties`() {
        contextRunner.run { context ->
            val properties = context.getBean(EmbeddingModelProperties::class.java)

            properties.id shouldBe "custom-embedding"
            properties.version shouldBe "2026-01"
            properties.runtime shouldBe EmbeddingModelRuntime.ONNX
            properties.modelPath shouldBe "/models/custom/model.onnx"
            properties.tokenizerPath shouldBe "/models/custom/tokenizer.json"
            properties.dimensions shouldBe 768
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddingModelProperties::class)
    private class PropertiesConfiguration
}
