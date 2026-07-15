package org.alterbit.aisme.embedding

data class EmbeddingModelConfigProperties(
    val enabled: Boolean = false,
    val displayName: String? = null,
    val version: String? = null,
    val dimensions: Int? = null,
    val runtime: EmbeddingModelRuntimeProperties = EmbeddingModelRuntimeProperties(),
) {
    init {
        require(displayName == null || displayName.isNotBlank()) {
            "aisme.embedding.models.display-name must not be blank when configured"
        }
        require(version == null || version.isNotBlank()) {
            "aisme.embedding.models.version must not be blank when configured"
        }
        require(dimensions == null || dimensions > 0) {
            "aisme.embedding.models.dimensions must be greater than 0 when configured"
        }
    }
}
