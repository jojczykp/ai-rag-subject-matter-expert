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
            "aisme.embedded-llama.asset-directory=/opt/aisme/models/llama",
            "aisme.embedded-llama.server-executable-path=/opt/aisme/bin/llama-server",
            "aisme.embedded-llama.host=127.0.0.2",
            "aisme.embedded-llama.port=19090",
        )

    @Test
    fun `uses configured embedded llama properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<EmbeddedLlamaProperties>()

            properties.assetDirectory shouldBe "/opt/aisme/models/llama"
            properties.serverExecutablePath shouldBe "/opt/aisme/bin/llama-server"
            properties.host shouldBe "127.0.0.2"
            properties.port shouldBe 19090
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddedLlamaProperties::class)
    private class PropertiesConfiguration
}
