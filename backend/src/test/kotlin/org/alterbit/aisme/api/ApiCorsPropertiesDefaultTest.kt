package org.alterbit.aisme.api

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ApiCorsPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `uses default api cors properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<ApiCorsProperties>()

            properties.allowedOrigins shouldBe listOf("http://localhost:5173")
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ApiCorsProperties::class)
    private class PropertiesConfiguration
}
