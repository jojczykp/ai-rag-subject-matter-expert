package org.alterbit.aisme.embedding

data class EmbeddingModelRuntimeProperties(
    val id: String? = null,
) {
    init {
        require(id == null || id.isNotBlank()) {
            "aisme.embedding-models.runtime.id must not be blank"
        }
    }
}
