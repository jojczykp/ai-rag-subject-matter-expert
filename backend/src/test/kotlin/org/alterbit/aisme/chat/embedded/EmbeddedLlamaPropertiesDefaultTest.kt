package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class EmbeddedLlamaPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.embedded-llama.asset-directory=./models/llama",
            "aisme.embedded-llama.server-executable-path=./models/llama/bin/llama-server",
            "aisme.embedded-llama.models[0].id=embedded-llama-tiny",
            "aisme.embedded-llama.models[0].enabled=false",
            "aisme.embedded-llama.models[0].display-name=Embedded Llama",
            "aisme.embedded-llama.models[0].gguf-file=models/llama.gguf",
            "aisme.embedded-llama.models[0].context-size=4096",
        )

    @Test
    fun `keeps embedded llama model disabled by default`() {
        contextRunner.run { context ->
            val properties = context.getBean<EmbeddedLlamaProperties>()

            properties.models.single().enabled shouldBe false
            properties.enabledModels() shouldBe emptyList()
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddedLlamaProperties::class)
    private class PropertiesConfiguration
}
