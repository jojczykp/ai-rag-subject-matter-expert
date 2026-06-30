package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class SubjectDocumentsPropertiesOverrideTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration::class.java)
        .withPropertyValues(
            "aisme.documents.location=classpath:/custom-documents/",
            "aisme.documents.chunk-size=2000",
            "aisme.documents.chunk-overlap=250",
        )

    @Test
    fun `uses configured document properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<SubjectDocumentsProperties>()

            properties.location shouldBe "classpath:/custom-documents/"
            properties.chunkSize shouldBe 2000
            properties.chunkOverlap shouldBe 250
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SubjectDocumentsProperties::class)
    private class PropertiesConfiguration
}
