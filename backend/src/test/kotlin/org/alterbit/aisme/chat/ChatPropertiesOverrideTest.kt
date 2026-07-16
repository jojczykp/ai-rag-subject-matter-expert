package org.alterbit.aisme.chat

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.alterbit.aisme.chat.catalog.ChatProperties
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ChatPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.chat.api-timeout=45s",
            "aisme.chat.retrieved-chunk-limit=3",
        )

    @Test
    fun `uses configured chat properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<ChatProperties>()

            properties.apiTimeout shouldBe Duration.ofSeconds(45)
            properties.retrievedChunkLimit shouldBe 3
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatProperties::class)
    private class PropertiesConfiguration
}
