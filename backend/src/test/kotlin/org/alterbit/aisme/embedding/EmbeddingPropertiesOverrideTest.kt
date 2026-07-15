package org.alterbit.aisme.embedding

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class EmbeddingPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.embedding.runtimes.custom-onnx.type=ONNX",
            "aisme.embedding.models.custom-embedding.enabled=true",
            "aisme.embedding.models.custom-embedding.version=2026-01",
            "aisme.embedding.models.custom-embedding.dimensions=768",
            "aisme.embedding.models.custom-embedding.runtime.id=custom-onnx",
            "aisme.embedding.models.custom-embedding.runtime.model-path=/models/custom/model.onnx",
            "aisme.embedding.models.custom-embedding.runtime.tokenizer-path=/models/custom/tokenizer.json",
        )

    @Test
    fun `uses configured embedding model properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<EmbeddingModelProperties>()

            properties.id shouldBe "custom-embedding"
            properties.version shouldBe "2026-01"
            properties.runtime shouldBe EmbeddingModelRuntime.ONNX
            properties.modelPath shouldBe "/models/custom/model.onnx"
            properties.tokenizerPath shouldBe "/models/custom/tokenizer.json"
            properties.dimensions shouldBe 768
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddingProperties::class)
    private class PropertiesConfiguration {
        @org.springframework.context.annotation.Bean
        fun embeddingModelProperties(properties: EmbeddingProperties): EmbeddingModelProperties =
            properties.activeModel()
    }
}
