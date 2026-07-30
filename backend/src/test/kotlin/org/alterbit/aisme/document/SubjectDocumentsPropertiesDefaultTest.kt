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
    fun `uses default subject document configuration`() {
        contextRunner.run { context ->
            val properties = context.getBean<SubjectsProperties>()
            val subject = properties.definitions.getValue("culinary-expert")

            properties.defaultSubjectId shouldBe "culinary-expert"
            subject.enabled shouldBe true
            subject.displayOrder shouldBe 10
            subject.displayName shouldBe "Culinary Expert"
            subject.defaultQuestion shouldBe "How should I cook rice?"
            subject.documents.location shouldBe "classpath:/subject_documents/culinary_expert/"
            subject.documents.chunkSize shouldBe 700
            subject.documents.chunkOverlap shouldBe 100
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SubjectsProperties::class)
    private class PropertiesConfiguration
}
