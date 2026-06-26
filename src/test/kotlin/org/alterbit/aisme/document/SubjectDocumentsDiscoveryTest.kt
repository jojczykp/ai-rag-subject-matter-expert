package org.alterbit.aisme.document

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("no-db")
@SpringBootTest
class SubjectDocumentsDiscoveryTest(
    private val discovery: SubjectDocumentsDiscovery,
) {
    @Test
    fun `discovers text documents recursively`() {
        val documents = discovery.discover()

        documents.map { it.relativePath } shouldBe listOf(
            "getting-started.txt",
            "nested/reference.txt",
        )
    }
}
