package org.alterbit.aisme.chat

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ChatPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.chat.timeout=45s",
        )

    @Test
    fun `uses configured chat timeout`() {
        contextRunner.run { context ->
            val properties = context.getBean<ChatProperties>()

            properties.timeout shouldBe Duration.ofSeconds(45)
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatProperties::class)
    private class PropertiesConfiguration
}
