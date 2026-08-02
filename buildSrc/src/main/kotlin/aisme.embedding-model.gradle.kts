val embeddingModelDirectory = layout.projectDirectory.dir("models/bge-small-en-v1.5")

tasks.register<Delete>("cleanLocalEmbeddingModelAssets") {
    group = "model management"
    description = "Deletes locally downloaded embedding model assets under backend/models/bge-small-en-v1.5."
    delete(embeddingModelDirectory)
}
