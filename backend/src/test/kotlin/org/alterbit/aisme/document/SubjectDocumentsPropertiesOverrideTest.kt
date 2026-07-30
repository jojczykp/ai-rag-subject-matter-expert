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
            "aisme.subjects.default-subject-id=finance",
            "aisme.subjects.definitions.finance.enabled=true",
            "aisme.subjects.definitions.finance.display-order=20",
            "aisme.subjects.definitions.finance.display-name=Finance",
            "aisme.subjects.definitions.finance.default-question=How should I review cash flow?",
            "aisme.subjects.definitions.finance.documents.location=classpath:/custom-documents/",
            "aisme.subjects.definitions.finance.documents.chunk-size=2000",
            "aisme.subjects.definitions.finance.documents.chunk-overlap=250",
        )

    @Test
    fun `uses configured subject document properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<SubjectsProperties>()
            val subject = properties.definitions.getValue("finance")

            properties.defaultSubjectId shouldBe "finance"
            subject.enabled shouldBe true
            subject.displayOrder shouldBe 20
            subject.displayName shouldBe "Finance"
            subject.defaultQuestion shouldBe "How should I review cash flow?"
            subject.documents.location shouldBe "classpath:/custom-documents/"
            subject.documents.chunkSize shouldBe 2000
            subject.documents.chunkOverlap shouldBe 250
        }
    }

    @Test
    fun `allows empty default question`() {
        contextRunner
            .withPropertyValues("aisme.subjects.definitions.finance.default-question=")
            .run { context ->
                val subject = context.getBean<SubjectsProperties>().definitions.getValue("finance")

                subject.defaultQuestion shouldBe ""
            }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SubjectsProperties::class)
    private class PropertiesConfiguration
}
