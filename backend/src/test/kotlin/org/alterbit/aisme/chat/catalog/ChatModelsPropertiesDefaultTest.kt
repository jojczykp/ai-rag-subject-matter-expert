package org.alterbit.aisme.chat.catalog

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ChatModelsPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `fails binding without configured runtimes and chat models`() {
        contextRunner.run { context ->
            context.startupFailure
                .shouldNotBeNull()
                .stackTraceToString() shouldContain "ChatModelsProperties"
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatModelsProperties::class)
    private class PropertiesConfiguration
}
