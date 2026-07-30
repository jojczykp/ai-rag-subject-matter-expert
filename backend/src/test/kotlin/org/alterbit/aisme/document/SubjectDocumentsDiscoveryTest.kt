package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class SubjectDocumentsDiscoveryTest {
    @Test
    fun `discovers text documents recursively`() {
        val discovery = SubjectDocumentsDiscovery(
            resourcePatternResolver = PathMatchingResourcePatternResolver(),
        )

        val documents = discovery.discover(
            subject = SubjectDescriptor(
                id = "culinary-expert",
                enabled = true,
                displayOrder = 10,
                displayName = "Culinary Expert",
                defaultQuestion = "How should I cook rice?",
            ),
            documentsProperties = SubjectDocumentsProperties(
                location = "classpath:/subject_documents/culinary_expert/",
            ),
        )

        documents.map { it.subjectId }.distinct() shouldBe listOf("culinary-expert")
        documents.map { it.relativePath } shouldBe listOf(
            "getting-started.txt",
            "nested/reference.txt",
        )
    }
}
