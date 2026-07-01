package org.alterbit.aisme.chatmodel

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ConfiguredChatModelsPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `uses default configured model`() {
        contextRunner.run { context ->
            val properties = context.getBean<ConfiguredChatModelsProperties>()

            properties.chatModels shouldHaveSize 1
            val model = properties.chatModels.single()
            model.id shouldBe "local-ollama-llama"
            model.displayName shouldBe "Local Ollama Llama"
            model.runtime shouldBe ChatModelRuntime.OLLAMA
            model.mode shouldBe ChatModelMode.LOCAL_SERVER
            model.availableOffline shouldBe false
            model.baseUrl shouldBe "http://localhost:11434"
            model.modelName shouldBe "llama3.2"
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConfiguredChatModelsProperties::class)
    private class PropertiesConfiguration
}
