package org.alterbit.aisme.document

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.documents")
data class SubjectDocumentsProperties(
    val location: String = "classpath:/subject-documents/",
)
