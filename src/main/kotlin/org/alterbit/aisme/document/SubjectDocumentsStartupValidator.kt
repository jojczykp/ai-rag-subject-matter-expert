package org.alterbit.aisme.document

import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class SubjectDocumentsStartupValidator(
    private val properties: SubjectDocumentsProperties,
    private val discovery: SubjectDocumentsDiscovery,
    private val documentValidator: SubjectDocumentValidator,
    private val resourcePatternResolver: ResourcePatternResolver,
) : SmartInitializingSingleton {
    override fun afterSingletonsInstantiated() {
        validate()
    }

    fun validate() {
        val location = properties.normalizedLocation()
        val root = resourcePatternResolver.getResource(location)
        if (!root.exists()) {
            throw SubjectDocumentsException("Subject documents location does not exist: $location")
        }

        val documents = discovery.discover()
        if (documents.isEmpty()) {
            throw SubjectDocumentsException("No supported .txt subject documents found under: $location")
        }

        documents.forEach(documentValidator::validate)
    }
}
