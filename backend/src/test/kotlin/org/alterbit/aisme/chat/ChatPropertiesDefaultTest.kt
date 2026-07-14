package org.alterbit.aisme.chat

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ChatPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `uses default chat properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<ChatProperties>()

            properties.apiTimeout shouldBe Duration.ofSeconds(60)
            properties.relevantChunkLimit shouldBe 5
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatProperties::class)
    private class PropertiesConfiguration
}
