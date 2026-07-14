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
            "aisme.runtimes.openai-compatible.type=OPENAI_COMPATIBLE",
            "aisme.runtimes.openai-compatible.base-url=https://api.example.com/v1",
            "aisme.runtimes.openai-compatible.api-key=test-api-key",
            "aisme.runtimes.embedded-llama.type=EMBEDDED_OFFLINE",
            "aisme.runtimes.embedded-llama.asset-directory=./models/llama",
            "aisme.runtimes.embedded-llama.server-executable-path=./models/llama/bin/llama-server",
            "aisme.chat-models[0].id=cloud-gpt",
            "aisme.chat-models[0].enabled=true",
            "aisme.chat-models[0].display-name=Cloud GPT",
            "aisme.chat-models[0].runtime-id=openai-compatible",
            "aisme.chat-models[0].model-name=gpt-4.1-mini",
            "aisme.chat-models[1].id=embedded-qwen",
            "aisme.chat-models[1].enabled=true",
            "aisme.chat-models[1].display-name=Embedded Qwen",
            "aisme.chat-models[1].runtime-id=embedded-llama",
            "aisme.chat-models[1].model-name=qwen2.5",
            "aisme.chat-models[1].gguf-file=models/qwen.gguf",
            "aisme.chat-models[1].context-size=2048",
        )

    @Test
    fun `uses configured models`() {
        contextRunner.run { context ->
            val properties = context.getBean<ConfiguredChatModelsProperties>()

            properties.chatModels shouldHaveSize 2
            val cloudModel = properties.chatModels[0]
            val cloudRuntime = properties.runtimes.getValue(cloudModel.requireRuntimeId())
            cloudModel.id shouldBe "cloud-gpt"
            cloudModel.enabled shouldBe true
            cloudModel.displayName shouldBe "Cloud GPT"
            cloudModel.runtimeId shouldBe "openai-compatible"
            cloudModel.modelName shouldBe "gpt-4.1-mini"
            cloudRuntime.type shouldBe ChatModelRuntime.OPENAI_COMPATIBLE
            cloudRuntime.mode shouldBe ChatModelMode.ONLINE
            cloudRuntime.availableOffline shouldBe false
            cloudRuntime.baseUrl shouldBe "https://api.example.com/v1"
            cloudRuntime.apiKey shouldBe "test-api-key"

            val embeddedModel = properties.chatModels[1]
            val embeddedRuntime = properties.runtimes.getValue(embeddedModel.requireRuntimeId())
            embeddedModel.id shouldBe "embedded-qwen"
            embeddedModel.enabled shouldBe true
            embeddedModel.displayName shouldBe "Embedded Qwen"
            embeddedModel.runtimeId shouldBe "embedded-llama"
            embeddedModel.modelName shouldBe "qwen2.5"
            embeddedModel.ggufFile shouldBe "models/qwen.gguf"
            embeddedModel.contextSize shouldBe 2048
            embeddedRuntime.type shouldBe ChatModelRuntime.EMBEDDED_OFFLINE
            embeddedRuntime.mode shouldBe ChatModelMode.EMBEDDED_OFFLINE
            embeddedRuntime.availableOffline shouldBe true
            embeddedRuntime.baseUrl shouldBe null
            embeddedRuntime.apiKey shouldBe null
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConfiguredChatModelsProperties::class)
    private class PropertiesConfiguration
}
