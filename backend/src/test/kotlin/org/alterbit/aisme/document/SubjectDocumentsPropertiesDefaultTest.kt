package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class SubjectDocumentsPropertiesDefaultTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `uses default document configuration`() {
        contextRunner.run { context ->
            val properties = context.getBean<SubjectDocumentsProperties>()

            properties.location shouldBe "classpath:/subject-documents/"
            properties.chunkSize shouldBe 700
            properties.chunkOverlap shouldBe 100
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SubjectDocumentsProperties::class)
    private class PropertiesConfiguration
}
