import java.net.URI

val embeddingModelDirectory = layout.projectDirectory.dir("models/bge-small-en-v1.5")

val embeddingModelAssets = listOf(
    EmbeddingModelAsset(
        fileName = "model.onnx",
        url = "https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/onnx/model.onnx",
    ),
    EmbeddingModelAsset(
        fileName = "tokenizer.json",
        url = "https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/tokenizer.json",
    ),
)

data class EmbeddingModelAsset(
    val fileName: String,
    val url: String,
)

tasks.register("embeddingModelDownload") {
    group = "model management"
    description = "Downloads the local ONNX embedding model assets when they are missing."

    doLast {
        embeddingModelAssets.forEach { asset ->
            val targetFile = embeddingModelDirectory.file(asset.fileName).asFile
            if (targetFile.isFile) {
                logger.lifecycle("Embedding model asset already exists: ${targetFile.path}")
                return@forEach
            }

            targetFile.parentFile.mkdirs()
            logger.lifecycle("Downloading embedding model asset to ${targetFile.path}")
            URI(asset.url).toURL().openStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

tasks.register<Delete>("cleanEmbeddingModel") {
    group = "model management"
    description = "Deletes locally downloaded embedding model assets under backend/models/bge-small-en-v1.5."
    delete(embeddingModelDirectory)
}
