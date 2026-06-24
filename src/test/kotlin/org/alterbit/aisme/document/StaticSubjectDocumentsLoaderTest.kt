package org.alterbit.aisme.document

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.AbstractResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class StaticSubjectDocumentsLoaderTest {
    private val resourcePatternResolver = PathMatchingResourcePatternResolver()
    private val documentReader = SubjectDocumentReader()
    private val documentValidator = SubjectDocumentValidator()

    @Test
    fun `loads readable non-empty text documents`(@TempDir documentsDirectory: Path) {
        Files.writeString(documentsDirectory.resolve("reference.txt"), "Reference content")

        val chunks = loader(documentsDirectory).load()

        chunks shouldHaveSize 1
    }

    @Test
    fun `fails when configured location is missing`(@TempDir tempDirectory: Path) {
        val missingDirectory = tempDirectory.resolve("missing")

        val exception = shouldThrow<SubjectDocumentsException> {
            loader(missingDirectory).load()
        }

        exception.message shouldContain "does not exist"
    }

    @Test
    fun `fails when no supported text documents are found`(@TempDir documentsDirectory: Path) {
        Files.writeString(documentsDirectory.resolve("ignored.md"), "# Ignored")

        val exception = shouldThrow<SubjectDocumentsException> {
            loader(documentsDirectory).load()
        }

        exception.message shouldContain "No supported .txt"
    }

    @Test
    fun `fails when a text document is empty`(@TempDir documentsDirectory: Path) {
        Files.writeString(documentsDirectory.resolve("empty.txt"), "   ")

        val exception = shouldThrow<SubjectDocumentsException> {
            loader(documentsDirectory).load()
        }

        exception.message shouldContain "empty"
    }

    @Test
    fun `fails when a discovered document is unreadable`() {
        val unreadableDocument = SubjectDocumentResource(
            relativePath = "reference.txt",
            resource = UnreadableResource,
        )

        val exception = shouldThrow<SubjectDocumentsException> {
            documentValidator.validate(unreadableDocument)
        }

        exception.message shouldContain "not readable"
    }

    private fun loader(
        documentsDirectory: Path,
    ): StaticSubjectDocumentsLoader =
        StaticSubjectDocumentsLoader(
            properties = properties(documentsDirectory),
            discovery = SubjectDocumentsDiscovery(
                properties = properties(documentsDirectory),
                resourcePatternResolver = resourcePatternResolver,
            ),
            documentReader = documentReader,
            documentValidator = documentValidator,
            documentChunker = SubjectDocumentChunker(properties(documentsDirectory)),
            resourcePatternResolver = resourcePatternResolver,
        )

    private fun properties(documentsDirectory: Path): SubjectDocumentsProperties =
        SubjectDocumentsProperties(location = documentsDirectory.toUri().toString())

    private object UnreadableResource : AbstractResource() {
        override fun getDescription(): String =
            "unreadable test resource"

        override fun exists(): Boolean =
            true

        override fun isReadable(): Boolean =
            false

        override fun getInputStream(): InputStream =
            ByteArrayInputStream(ByteArray(0))
    }
}
