package org.alterbit.aisme.modelcatalog

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ChatModelsPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.chat.runtimes.openai-compatible.type=OPENAI_COMPATIBLE",
            "aisme.chat.runtimes.openai-compatible.base-url=https://api.example.com/v1",
            "aisme.chat.runtimes.openai-compatible.api-key=test-api-key",
            "aisme.chat.runtimes.embedded-llama.type=EMBEDDED_LLAMA",
            "aisme.chat.runtimes.embedded-llama.asset-directory=./models/llama",
            "aisme.chat.runtimes.embedded-llama.server-executable-path=./models/llama/bin/llama-server",
            "aisme.chat.models.cloud-gpt.enabled=true",
            "aisme.chat.models.cloud-gpt.display-order=10",
            "aisme.chat.models.cloud-gpt.display-name=Cloud GPT",
            "aisme.chat.models.cloud-gpt.runtime.id=openai-compatible",
            "aisme.chat.models.cloud-gpt.runtime.model-name=gpt-4.1-mini",
            "aisme.chat.models.embedded-qwen.enabled=true",
            "aisme.chat.models.embedded-qwen.display-order=20",
            "aisme.chat.models.embedded-qwen.display-name=Embedded Qwen",
            "aisme.chat.models.embedded-qwen.runtime.id=embedded-llama",
            "aisme.chat.models.embedded-qwen.runtime.model-name=qwen2.5",
            "aisme.chat.models.embedded-qwen.runtime.gguf-file=models/qwen.gguf",
            "aisme.chat.models.embedded-qwen.runtime.context-size=2048",
        )

    @Test
    fun `uses configured models`() {
        contextRunner.run { context ->
            val properties = context.getBean<ChatModelsProperties>()

            properties.chatModelsById.size shouldBe 2
            val cloudModel = properties.chatModelsById.getValue("cloud-gpt")
            val cloudRuntime = properties.chatRuntimesById.getValue(cloudModel.requireRuntimeId())
            cloudModel.enabled shouldBe true
            cloudModel.displayOrder shouldBe 10
            cloudModel.displayName shouldBe "Cloud GPT"
            cloudModel.runtime.id shouldBe "openai-compatible"
            cloudModel.runtime.modelName shouldBe "gpt-4.1-mini"
            cloudRuntime.type shouldBe ChatModelRuntime.OPENAI_COMPATIBLE
            cloudRuntime.mode shouldBe ChatModelMode.ONLINE
            cloudRuntime.availableOffline shouldBe false
            cloudRuntime.baseUrl shouldBe "https://api.example.com/v1"
            cloudRuntime.apiKey shouldBe "test-api-key"

            val embeddedModel = properties.chatModelsById.getValue("embedded-qwen")
            val embeddedRuntime = properties.chatRuntimesById.getValue(embeddedModel.requireRuntimeId())
            embeddedModel.enabled shouldBe true
            embeddedModel.displayOrder shouldBe 20
            embeddedModel.displayName shouldBe "Embedded Qwen"
            embeddedModel.runtime.id shouldBe "embedded-llama"
            embeddedModel.runtime.modelName shouldBe "qwen2.5"
            embeddedModel.runtime.ggufFile shouldBe "models/qwen.gguf"
            embeddedModel.runtime.contextSize shouldBe 2048
            embeddedRuntime.type shouldBe ChatModelRuntime.EMBEDDED_LLAMA
            embeddedRuntime.mode shouldBe ChatModelMode.EMBEDDED_OFFLINE
            embeddedRuntime.availableOffline shouldBe true
            embeddedRuntime.baseUrl shouldBe null
            embeddedRuntime.apiKey shouldBe null
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatModelsProperties::class)
    private class PropertiesConfiguration
}
