package org.alterbit.aisme.api

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ApiCorsPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.api.cors.allowed-origins[0]=http://localhost:3000",
            "aisme.api.cors.allowed-origins[1]=https://ui.example.com",
        )

    @Test
    fun `uses configured api cors properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<ApiCorsProperties>()

            properties.allowedOrigins shouldBe listOf(
                "http://localhost:3000",
                "https://ui.example.com",
            )
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ApiCorsProperties::class)
    private class PropertiesConfiguration
}
