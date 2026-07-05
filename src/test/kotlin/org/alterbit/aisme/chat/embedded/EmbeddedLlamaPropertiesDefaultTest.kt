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

    @Test
    fun `uses default embedded llama configuration`() {
        contextRunner.run { context ->
            val properties = context.getBean<EmbeddedLlamaProperties>()

            properties.assetDirectory shouldBe "./models/llama"
            properties.serverExecutablePath shouldBe "./bin/llama-server"
            properties.host shouldBe "127.0.0.1"
            properties.port shouldBe 18080

            val model = properties.models.single()
            model.id shouldBe "embedded-llama"
            model.displayName shouldBe "Embedded Llama"
            model.ggufFile shouldBe "models/llama.gguf"
            model.contextSize shouldBe 4096
            model.runtimeArguments shouldBe emptyList()
            model.sha256 shouldBe null
            model.license shouldBe "TODO"
            model.hardwareRequirements shouldBe "TODO"
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddedLlamaProperties::class)
    private class PropertiesConfiguration
}
