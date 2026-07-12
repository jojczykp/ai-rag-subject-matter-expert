package org.alterbit.aisme.embedding

data class EmbeddingModelMetadata(
    val id: String,
    val version: String,
    val dimensions: Int,
) {
    init {
        require(id.isNotBlank()) { "embedding model id must not be blank" }
        require(version.isNotBlank()) { "embedding model version must not be blank" }
        require(dimensions > 0) { "embedding model dimensions must be greater than 0" }
    }
}
