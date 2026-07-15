package org.alterbit.aisme.embedding

data class EmbeddingModelProperties(
    val id: String = "local-bge-small",
    val version: String = "1.5",
    val dimensions: Int = 384,
    val runtime: EmbeddingModelRuntime = EmbeddingModelRuntime.ONNX,
    val modelPath: String = "./models/bge-small-en-v1.5/model.onnx",
    val tokenizerPath: String = "./models/bge-small-en-v1.5/tokenizer.json",
    val baseUrl: String? = null,
    val modelName: String? = null,
) {
    val metadata: EmbeddingModelMetadata
        get() = EmbeddingModelMetadata(
            id = id,
            version = version,
            dimensions = dimensions,
        )

    init {
        require(id.isNotBlank()) { "embedding model id must not be blank" }
        require(version.isNotBlank()) { "embedding model version must not be blank" }
        require(dimensions > 0) { "embedding model dimensions must be greater than 0" }
        when (runtime) {
            EmbeddingModelRuntime.ONNX -> {
                require(modelPath.isNotBlank()) { "embedding model model-path must not be blank" }
                require(tokenizerPath.isNotBlank()) { "embedding model tokenizer-path must not be blank" }
            }

            EmbeddingModelRuntime.OLLAMA -> {
                require(!baseUrl.isNullOrBlank()) { "embedding model base-url must not be blank" }
                require(!modelName.isNullOrBlank()) { "embedding model model-name must not be blank" }
            }
        }
    }
}
