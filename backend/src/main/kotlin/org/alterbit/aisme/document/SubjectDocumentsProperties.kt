package org.alterbit.aisme.document

data class SubjectDocumentsProperties(
    val location: String = "classpath:/subject_documents/",
    val chunkSize: Int = 700,
    val chunkOverlap: Int = 100,
) {
    init {
        require(location.isNotBlank()) { "subject documents location must not be blank" }
        require(chunkSize > 0) { "subject documents chunk-size must be greater than 0" }
        require(chunkOverlap >= 0) { "subject documents chunk-overlap must be greater than or equal to 0" }
        require(chunkOverlap < chunkSize) { "subject documents chunk-overlap must be smaller than chunk-size" }
    }

    fun normalizedLocation(): String =
        location.trimEnd('/') + "/"

    fun chunkingStrategyVersion(): String =
        "character-count-v1:size=$chunkSize:overlap=$chunkOverlap"
}
