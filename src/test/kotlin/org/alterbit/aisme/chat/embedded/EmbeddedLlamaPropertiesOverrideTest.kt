package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class EmbeddedLlamaPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.embedded-llama.enabled=true",
            "aisme.embedded-llama.config.asset-directory=/opt/aisme/models/llama",
            "aisme.embedded-llama.config.server-executable-path=/opt/aisme/bin/llama-server",
            "aisme.embedded-llama.config.models[0].id=embedded-mistral",
            "aisme.embedded-llama.config.models[0].display-name=Embedded Mistral",
            "aisme.embedded-llama.config.models[0].gguf-file=mistral/mistral-7b-instruct-q4.gguf",
            "aisme.embedded-llama.config.models[0].context-size=8192",
            "aisme.embedded-llama.config.models[0].runtime-arguments[0]=--threads",
            "aisme.embedded-llama.config.models[0].runtime-arguments[1]=8",
            "aisme.embedded-llama.config.models[0].sha256=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        )

    @Test
    fun `uses configured embedded llama properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<EmbeddedLlamaProperties>()
            val config = properties.requireEnabledConfig()

            properties.enabled shouldBe true
            config.assetDirectory shouldBe "/opt/aisme/models/llama"
            config.serverExecutablePath shouldBe "/opt/aisme/bin/llama-server"
            val model = config.models.single()
            model.id shouldBe "embedded-mistral"
            model.displayName shouldBe "Embedded Mistral"
            model.ggufFile shouldBe "mistral/mistral-7b-instruct-q4.gguf"
            model.contextSize shouldBe 8192
            model.runtimeArguments shouldBe listOf("--threads", "8")
            model.sha256 shouldBe "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddedLlamaProperties::class)
    private class PropertiesConfiguration
}
