val embeddingModelDirectory = layout.projectDirectory.dir("models/bge-small-en-v1.5")
val ollamaNomicEmbedModel = "nomic-embed-text:v1.5"

tasks.register<Delete>("cleanLocalEmbeddingModelAssets") {
    group = "model management"
    description = "Deletes locally downloaded embedding model assets under backend/models/bge-small-en-v1.5."
    delete(embeddingModelDirectory)
}

tasks.register<Exec>("embeddingModelPullOllamaNomicEmbed") {
    group = "model management"
    description = "Pulls the pinned Ollama Nomic Embed model used by ollama-nomic-embed."
    commandLine("ollama", "pull", ollamaNomicEmbedModel)
}
