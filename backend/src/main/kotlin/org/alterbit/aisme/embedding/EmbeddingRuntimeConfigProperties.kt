package org.alterbit.aisme.embedding

data class EmbeddingRuntimeConfigProperties(
    val type: EmbeddingModelRuntime,
    val baseUrl: String? = null,
) {
    init {
        require(baseUrl == null || baseUrl.isNotBlank()) {
            "aisme.embedding.runtimes.base-url must not be blank when configured"
        }
    }
}
