package org.alterbit.aisme.document

import org.springframework.core.io.support.ResourcePatternResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class StaticSubjectDocumentsLoader(
    private val properties: SubjectDocumentsProperties,
    private val resourcePatternResolver: ResourcePatternResolver,
    private val discovery: SubjectDocumentsDiscovery,
    private val documentReader: SubjectDocumentReader,
    private val documentValidator: SubjectDocumentValidator,
    private val documentChunker: SubjectDocumentChunker,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun load(): List<SubjectDocumentChunk> {
        val location = properties.normalizedLocation()
        logger.info("Loading static subject documents from '{}'", location)
        val root = resourcePatternResolver.getResource(location)
        if (!root.exists()) {
            logger.warn("Static subject documents location does not exist: '{}'", location)
            throw SubjectDocumentsException("Subject documents location does not exist: $location")
        }

        val documents = discovery.discover()
        if (documents.isEmpty()) {
            logger.warn("No supported .txt subject documents found under '{}'", location)
            throw SubjectDocumentsException("No supported .txt subject documents found under: $location")
        }
        logger.info("Discovered {} supported static subject document(s)", documents.size)

        val chunks = documents
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
        logger.info("Loaded {} static subject document(s) into {} chunk(s)", documents.size, chunks.size)

        return chunks
    }
}
