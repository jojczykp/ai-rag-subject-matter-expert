package org.alterbit.aisme.document

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.documents")
data class SubjectDocumentsProperties(
    val location: String = "classpath:/subject-documents/",
    val chunkSize: Int = 700,
    val chunkOverlap: Int = 100,
) {
    init {
        require(chunkSize > 0) { "aisme.documents.chunk-size must be greater than 0" }
        require(chunkOverlap >= 0) { "aisme.documents.chunk-overlap must be greater than or equal to 0" }
        require(chunkOverlap < chunkSize) { "aisme.documents.chunk-overlap must be smaller than chunk-size" }
    }

    fun normalizedLocation(): String =
        location.trimEnd('/') + "/"
}
