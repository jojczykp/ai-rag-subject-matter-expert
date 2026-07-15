package org.alterbit.aisme.chat.catalog

import io.kotest.matchers.shouldBe
import java.time.Duration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ChatModelAvailabilityPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.chat.model-availability.timeout=2s",
            "aisme.chat.model-availability.cache-ttl=3s",
        )

    @Test
    fun `uses configured availability timeout`() {
        contextRunner.run { context ->
            val properties = context.getBean<ChatModelAvailabilityProperties>()

            properties.timeout shouldBe Duration.ofSeconds(2)
            properties.cacheTtl shouldBe Duration.ofSeconds(3)
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatModelAvailabilityProperties::class)
    private class PropertiesConfiguration
}
