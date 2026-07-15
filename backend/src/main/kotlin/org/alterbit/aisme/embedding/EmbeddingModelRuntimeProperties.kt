package org.alterbit.aisme.embedding

data class EmbeddingModelRuntimeProperties(
    val id: String? = null,
    val modelName: String? = null,
    val modelPath: String? = null,
    val tokenizerPath: String? = null,
) {
    init {
        require(id == null || id.isNotBlank()) {
            "aisme.embedding.models.runtime.id must not be blank"
        }
        require(modelName == null || modelName.isNotBlank()) {
            "aisme.embedding.models.runtime.model-name must not be blank when configured"
        }
        require(modelPath == null || modelPath.isNotBlank()) {
            "aisme.embedding.models.runtime.model-path must not be blank when configured"
        }
        require(tokenizerPath == null || tokenizerPath.isNotBlank()) {
            "aisme.embedding.models.runtime.tokenizer-path must not be blank when configured"
        }
    }
}
