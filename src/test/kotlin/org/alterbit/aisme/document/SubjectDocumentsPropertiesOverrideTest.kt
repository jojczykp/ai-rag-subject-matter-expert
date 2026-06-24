package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "aisme.documents.location=classpath:/custom-documents/",
        "aisme.documents.chunk-size=2000",
        "aisme.documents.chunk-overlap=250",
    ],
)
class SubjectDocumentsPropertiesOverrideTest(
    private val properties: SubjectDocumentsProperties,
) {
    @Test
    fun `uses configured document properties`() {
        properties.location shouldBe "classpath:/custom-documents/"
        properties.chunkSize shouldBe 2000
        properties.chunkOverlap shouldBe 250
    }
}
