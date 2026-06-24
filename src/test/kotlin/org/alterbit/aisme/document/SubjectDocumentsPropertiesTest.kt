package org.alterbit.aisme.document

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class SubjectDocumentsPropertiesTest {
    @Test
    fun `rejects non-positive chunk size`() {
        val exception = shouldThrow<IllegalArgumentException> {
            SubjectDocumentsProperties(chunkSize = 0)
        }

        exception.message shouldContain "chunk-size"
    }

    @Test
    fun `rejects negative chunk overlap`() {
        val exception = shouldThrow<IllegalArgumentException> {
            SubjectDocumentsProperties(chunkOverlap = -1)
        }

        exception.message shouldContain "chunk-overlap"
    }

    @Test
    fun `rejects chunk overlap greater than or equal to chunk size`() {
        val exception = shouldThrow<IllegalArgumentException> {
            SubjectDocumentsProperties(chunkSize = 100, chunkOverlap = 100)
        }

        exception.message shouldContain "smaller than chunk-size"
    }
}
