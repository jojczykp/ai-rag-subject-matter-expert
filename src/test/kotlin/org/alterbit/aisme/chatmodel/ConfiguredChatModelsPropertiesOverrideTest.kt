package org.alterbit.aisme.chatmodel

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ConfiguredChatModelsPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.chat-models[0].id=cloud-gpt",
            "aisme.chat-models[0].display-name=Cloud GPT",
            "aisme.chat-models[0].runtime=SPRING_AI",
            "aisme.chat-models[0].mode=ONLINE",
            "aisme.chat-models[0].available-offline=false",
            "aisme.chat-models[1].id=embedded-llama",
            "aisme.chat-models[1].display-name=Embedded Llama",
            "aisme.chat-models[1].runtime=EMBEDDED_OFFLINE",
            "aisme.chat-models[1].mode=EMBEDDED_OFFLINE",
            "aisme.chat-models[1].available-offline=true",
        )

    @Test
    fun `uses configured models`() {
        contextRunner.run { context ->
            val properties = context.getBean(ConfiguredChatModelsProperties::class.java)

            properties.chatModels shouldHaveSize 2
            val cloudModel = properties.chatModels[0]
            cloudModel.id shouldBe "cloud-gpt"
            cloudModel.displayName shouldBe "Cloud GPT"
            cloudModel.runtime shouldBe ChatModelRuntime.SPRING_AI
            cloudModel.mode shouldBe ChatModelMode.ONLINE
            cloudModel.availableOffline shouldBe false
            cloudModel.baseUrl shouldBe null

            val embeddedModel = properties.chatModels[1]
            embeddedModel.id shouldBe "embedded-llama"
            embeddedModel.displayName shouldBe "Embedded Llama"
            embeddedModel.runtime shouldBe ChatModelRuntime.EMBEDDED_OFFLINE
            embeddedModel.mode shouldBe ChatModelMode.EMBEDDED_OFFLINE
            embeddedModel.availableOffline shouldBe true
            embeddedModel.baseUrl shouldBe null
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConfiguredChatModelsProperties::class)
    private class PropertiesConfiguration
}
