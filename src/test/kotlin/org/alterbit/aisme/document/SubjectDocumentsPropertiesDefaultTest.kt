package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SubjectDocumentsPropertiesDefaultTest(
    private val properties: SubjectDocumentsProperties,
) {
    @Test
    fun `uses default bundled document location`() {
        properties.location shouldBe "classpath:/subject-documents/"
    }
}
