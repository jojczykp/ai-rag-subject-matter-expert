package org.alterbit.aisme.document

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class StaticSubjectDocumentsLoader(
    private val properties: SubjectDocumentsProperties,
    private val resourcePatternResolver: ResourcePatternResolver,
    private val discovery: SubjectDocumentsDiscovery,
    private val documentReader: SubjectDocumentReader,
    private val documentValidator: SubjectDocumentValidator,
    private val documentChunker: SubjectDocumentChunker,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        load()
    }

    fun load(): List<SubjectDocumentChunk> {
        val location = properties.normalizedLocation()
        val root = resourcePatternResolver.getResource(location)
        if (!root.exists()) {
            throw SubjectDocumentsException("Subject documents location does not exist: $location")
        }

        val documents = discovery.discover()
        if (documents.isEmpty()) {
            throw SubjectDocumentsException("No supported .txt subject documents found under: $location")
        }

        return documents
            .onEach(documentValidator::validate)
            .map(documentReader::read)
            .onEach(documentValidator::validate)
            .flatMap { document ->
                try {
                    documentChunker.chunk(document)
                } catch (ex: RuntimeException) {
                    throw SubjectDocumentsException("Subject document chunking failed: ${document.relativePath}", ex)
                }
            }
    }
}
