package org.alterbit.aisme.modelcatalog

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ConfiguredChatModelsPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.chat-models[0].id=cloud-gpt",
            "aisme.chat-models[0].enabled=true",
            "aisme.chat-models[0].config.display-name=Cloud GPT",
            "aisme.chat-models[0].config.runtime=OPENAI_COMPATIBLE",
            "aisme.chat-models[0].config.mode=ONLINE",
            "aisme.chat-models[0].config.available-offline=false",
            "aisme.chat-models[0].config.base-url=https://api.example.com/v1",
            "aisme.chat-models[0].config.model-name=gpt-4.1-mini",
            "aisme.chat-models[0].config.api-key=test-api-key",
            "aisme.chat-models[1].id=embedded-qwen",
            "aisme.chat-models[1].enabled=true",
            "aisme.chat-models[1].config.display-name=Embedded Qwen",
            "aisme.chat-models[1].config.runtime=EMBEDDED_OFFLINE",
            "aisme.chat-models[1].config.mode=EMBEDDED_OFFLINE",
            "aisme.chat-models[1].config.available-offline=true",
        )

    @Test
    fun `uses configured models`() {
        contextRunner.run { context ->
            val properties = context.getBean<ConfiguredChatModelsProperties>()

            properties.chatModels shouldHaveSize 2
            val cloudModel = properties.chatModels[0]
            val cloudConfig = cloudModel.requireEnabledConfig()
            cloudModel.id shouldBe "cloud-gpt"
            cloudModel.enabled shouldBe true
            cloudConfig.displayName shouldBe "Cloud GPT"
            cloudConfig.runtime shouldBe ChatModelRuntime.OPENAI_COMPATIBLE
            cloudConfig.mode shouldBe ChatModelMode.ONLINE
            cloudConfig.availableOffline shouldBe false
            cloudConfig.baseUrl shouldBe "https://api.example.com/v1"
            cloudConfig.modelName shouldBe "gpt-4.1-mini"
            cloudConfig.apiKey shouldBe "test-api-key"

            val embeddedModel = properties.chatModels[1]
            val embeddedConfig = embeddedModel.requireEnabledConfig()
            embeddedModel.id shouldBe "embedded-qwen"
            embeddedModel.enabled shouldBe true
            embeddedConfig.displayName shouldBe "Embedded Qwen"
            embeddedConfig.runtime shouldBe ChatModelRuntime.EMBEDDED_OFFLINE
            embeddedConfig.mode shouldBe ChatModelMode.EMBEDDED_OFFLINE
            embeddedConfig.availableOffline shouldBe true
            embeddedConfig.baseUrl shouldBe null
            embeddedConfig.modelName shouldBe null
            embeddedConfig.apiKey shouldBe null
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConfiguredChatModelsProperties::class)
    private class PropertiesConfiguration
}
