package org.alterbit.aisme.embedding

data class EmbeddingRuntimeConfigProperties(
    val type: EmbeddingModelRuntime,
    val modelPath: String? = null,
    val tokenizerPath: String? = null,
) {
    init {
        require(modelPath == null || modelPath.isNotBlank()) {
            "aisme.embedding-runtimes.model-path must not be blank when configured"
        }
        require(tokenizerPath == null || tokenizerPath.isNotBlank()) {
            "aisme.embedding-runtimes.tokenizer-path must not be blank when configured"
        }
    }
}
