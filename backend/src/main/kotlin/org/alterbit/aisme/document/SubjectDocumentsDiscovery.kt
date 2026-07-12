package org.alterbit.aisme.document

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class SubjectDocumentsDiscovery(
    private val properties: SubjectDocumentsProperties,
    private val resourcePatternResolver: ResourcePatternResolver,
) {
    fun discover(): List<SubjectDocumentResource> =
        resourcePatternResolver
            .getResources("${properties.normalizedLocation()}**/*.txt")
            .map { resource ->
                SubjectDocumentResource(
                    relativePath = relativePath(resource.url.toString()),
                    resource = resource,
                )
            }
            .sortedBy { it.relativePath }

    private fun relativePath(resourceUrl: String): String {
        val normalizedLocation = properties.normalizedLocation()
        val rootPath = normalizedLocation.substringAfter("classpath:", normalizedLocation)
        val resourcePath = resourceUrl.substringAfter(rootPath)
        return URLDecoder.decode(resourcePath, StandardCharsets.UTF_8)
    }
}
