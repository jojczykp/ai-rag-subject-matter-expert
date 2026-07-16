package org.alterbit.aisme.document

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class SubjectDocumentsDiscovery(
    private val resourcePatternResolver: ResourcePatternResolver,
) {
    fun discover(
        subject: SubjectDescriptor,
        documentsProperties: SubjectDocumentsProperties,
    ): List<SubjectDocumentResource> =
        resourcePatternResolver
            .getResources("${documentsProperties.normalizedLocation()}**/*.txt")
            .map { resource ->
                SubjectDocumentResource(
                    subjectId = subject.id,
                    relativePath = relativePath(
                        resourceUrl = resource.url.toString(),
                        documentsProperties = documentsProperties,
                    ),
                    resource = resource,
                )
            }
            .sortedBy(SubjectDocumentResource::relativePath)

    private fun relativePath(
        resourceUrl: String,
        documentsProperties: SubjectDocumentsProperties,
    ): String {
        val normalizedLocation = documentsProperties.normalizedLocation()
        val rootPath = normalizedLocation.substringAfter("classpath:", normalizedLocation)
        val resourcePath = resourceUrl.substringAfter(rootPath)
        return URLDecoder.decode(resourcePath, StandardCharsets.UTF_8)
    }
}
