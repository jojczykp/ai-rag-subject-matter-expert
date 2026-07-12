package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class SubjectDocumentsDiscoveryTest {
    @Test
    fun `discovers text documents recursively`() {
        val discovery = SubjectDocumentsDiscovery(
            properties = SubjectDocumentsProperties(location = "classpath:/subject-documents/"),
            resourcePatternResolver = PathMatchingResourcePatternResolver(),
        )

        val documents = discovery.discover()

        documents.map { it.relativePath } shouldBe listOf(
            "getting-started.txt",
            "nested/reference.txt",
        )
    }
}
