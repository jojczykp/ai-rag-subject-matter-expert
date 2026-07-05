package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class LlamaRuntimePropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `keeps llama runtime disabled by default`() {
        contextRunner.run { context ->
            val properties = context.getBean<LlamaRuntimeProperties>()

            properties.enabled shouldBe false
            properties.config shouldBe null
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LlamaRuntimeProperties::class)
    private class PropertiesConfiguration
}
