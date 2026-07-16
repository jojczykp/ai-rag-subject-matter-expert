package org.alterbit.aisme.document

import org.springframework.core.io.support.ResourcePatternResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class StaticSubjectDocumentsLoader(
    private val subjectsProperties: SubjectsProperties,
    private val resourcePatternResolver: ResourcePatternResolver,
    private val discovery: SubjectDocumentsDiscovery,
    private val documentReader: SubjectDocumentReader,
    private val documentValidator: SubjectDocumentValidator,
    private val documentChunker: SubjectDocumentChunker,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun load(): List<SubjectDocumentChunk> {
        val enabledSubjects = subjectsProperties.enabledSubjects()
        if (enabledSubjects.isEmpty()) {
            logger.warn("No enabled subjects are configured")
            throw SubjectDocumentsException("No enabled subjects are configured")
        }

        val chunks = enabledSubjects.flatMap { subject -> loadSubject(subject) }
        logger.info(
            "Loaded {} static subject(s) into {} chunk(s)",
            enabledSubjects.size,
            chunks.size,
        )

        return chunks
    }

    private fun loadSubject(subject: SubjectDescriptor): List<SubjectDocumentChunk> {
        val documentsProperties = subjectsProperties.documentsForSubjectOrThrow(subject.id)
        val location = documentsProperties.normalizedLocation()
        logger.info("Loading static subject documents for subject '{}' from '{}'", subject.id, location)
        val root = resourcePatternResolver.getResource(location)
        if (!root.exists()) {
            logger.warn("Static subject documents location does not exist for subject '{}': '{}'", subject.id, location)
            throw SubjectDocumentsException("Subject documents location does not exist for subject ${subject.id}: $location")
        }

        val documents = discovery.discover(
            subject = subject,
            documentsProperties = documentsProperties,
        )
        if (documents.isEmpty()) {
            logger.warn("No supported .txt subject documents found for subject '{}' under '{}'", subject.id, location)
            throw SubjectDocumentsException("No supported .txt subject documents found for subject ${subject.id}: $location")
        }
        logger.info("Discovered {} supported static document(s) for subject '{}'", documents.size, subject.id)

        return documents
            .onEach(documentValidator::validate)
            .map(documentReader::read)
            .onEach(documentValidator::validate)
            .flatMap { document ->
                try {
                    documentChunker.chunk(
                        document = document,
                        documentsProperties = documentsProperties,
                    )
                } catch (ex: RuntimeException) {
                    throw SubjectDocumentsException("Subject document chunking failed: ${document.relativePath}", ex)
                }
            }
    }
}
