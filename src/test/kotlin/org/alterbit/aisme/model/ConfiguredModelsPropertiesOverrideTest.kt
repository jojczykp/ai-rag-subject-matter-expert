package org.alterbit.aisme.model

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ConfiguredModelsPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.models[0].id=cloud-gpt",
            "aisme.models[0].display-name=Cloud GPT",
            "aisme.models[0].runtime=SPRING_AI",
            "aisme.models[0].mode=ONLINE",
            "aisme.models[0].available-offline=false",
            "aisme.models[1].id=embedded-llama",
            "aisme.models[1].display-name=Embedded Llama",
            "aisme.models[1].runtime=EMBEDDED_OFFLINE",
            "aisme.models[1].mode=EMBEDDED_OFFLINE",
            "aisme.models[1].available-offline=true",
        )

    @Test
    fun `uses configured models`() {
        contextRunner.run { context ->
            val properties = context.getBean(ConfiguredModelsProperties::class.java)

            properties.models shouldHaveSize 2
            val cloudModel = properties.models[0]
            cloudModel.id shouldBe "cloud-gpt"
            cloudModel.displayName shouldBe "Cloud GPT"
            cloudModel.runtime shouldBe ModelRuntime.SPRING_AI
            cloudModel.mode shouldBe ModelMode.ONLINE
            cloudModel.availableOffline shouldBe false
            cloudModel.baseUrl shouldBe null

            val embeddedModel = properties.models[1]
            embeddedModel.id shouldBe "embedded-llama"
            embeddedModel.displayName shouldBe "Embedded Llama"
            embeddedModel.runtime shouldBe ModelRuntime.EMBEDDED_OFFLINE
            embeddedModel.mode shouldBe ModelMode.EMBEDDED_OFFLINE
            embeddedModel.availableOffline shouldBe true
            embeddedModel.baseUrl shouldBe null
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConfiguredModelsProperties::class)
    private class PropertiesConfiguration
}
