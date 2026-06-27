package org.alterbit.aisme.embedding

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.embedding-model")
data class EmbeddingModelProperties(
    val id: String = "local-bge-small",
    val version: String = "1.5",
    val runtime: EmbeddingModelRuntime = EmbeddingModelRuntime.ONNX,
    val modelPath: String = "./models/bge-small-en-v1.5/model.onnx",
    val tokenizerPath: String = "./models/bge-small-en-v1.5/tokenizer.json",
    val dimensions: Int = 384,
) {
    init {
        require(id.isNotBlank()) { "aisme.embedding-model.id must not be blank" }
        require(version.isNotBlank()) { "aisme.embedding-model.version must not be blank" }
        require(modelPath.isNotBlank()) { "aisme.embedding-model.model-path must not be blank" }
        require(tokenizerPath.isNotBlank()) { "aisme.embedding-model.tokenizer-path must not be blank" }
        require(dimensions > 0) { "aisme.embedding-model.dimensions must be greater than 0" }
    }
}
