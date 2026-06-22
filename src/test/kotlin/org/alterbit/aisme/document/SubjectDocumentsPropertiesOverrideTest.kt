package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "aisme.documents.location=classpath:/custom-documents/",
    ],
)
class SubjectDocumentsPropertiesOverrideTest(
    private val properties: SubjectDocumentsProperties,
) {
    @Test
    fun `uses configured bundled document location`() {
        properties.location shouldBe "classpath:/custom-documents/"
    }
}
