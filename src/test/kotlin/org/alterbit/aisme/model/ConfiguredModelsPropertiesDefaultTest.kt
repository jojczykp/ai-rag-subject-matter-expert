package org.alterbit.aisme.model

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ConfiguredModelsPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `uses default configured model`() {
        contextRunner.run { context ->
            val properties = context.getBean(ConfiguredModelsProperties::class.java)

            properties.models shouldHaveSize 1
            val model = properties.models.single()
            model.id shouldBe "local-ollama-llama"
            model.displayName shouldBe "Local Ollama Llama"
            model.runtime shouldBe ModelRuntime.OLLAMA
            model.mode shouldBe ModelMode.LOCAL_SERVER
            model.availableOffline shouldBe false
            model.baseUrl shouldBe "http://localhost:11434"
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConfiguredModelsProperties::class)
    private class PropertiesConfiguration
}
