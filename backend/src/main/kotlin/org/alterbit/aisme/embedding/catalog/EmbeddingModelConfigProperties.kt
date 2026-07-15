package org.alterbit.aisme.embedding.catalog

data class EmbeddingModelConfigProperties(
    val enabled: Boolean = false,
    val displayOrder: Int? = null,
    val displayName: String? = null,
    val version: String? = null,
    val dimensions: Int? = null,
    val runtime: EmbeddingModelRuntimeProperties = EmbeddingModelRuntimeProperties(),
) {
    init {
        require(displayOrder == null || displayOrder >= 0) {
            "aisme.embedding.models.display-order must not be negative when configured"
        }
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
