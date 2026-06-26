package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("no-db")
@SpringBootTest
class SubjectDocumentsPropertiesDefaultTest(
    private val properties: SubjectDocumentsProperties,
) {
    @Test
    fun `uses default document configuration`() {
        properties.location shouldBe "classpath:/subject-documents/"
        properties.chunkSize shouldBe 700
        properties.chunkOverlap shouldBe 100
    }
}
