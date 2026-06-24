package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SubjectDocumentsPropertiesDefaultTest(
    private val properties: SubjectDocumentsProperties,
) {
    @Test
    fun `uses default document configuration`() {
        properties.location shouldBe "classpath:/subject-documents/"
        properties.chunkSize shouldBe 1000
        properties.chunkOverlap shouldBe 150
    }
}
