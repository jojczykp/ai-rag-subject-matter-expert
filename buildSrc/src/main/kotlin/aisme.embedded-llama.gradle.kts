val assetDirectory = layout.projectDirectory.dir("models/llama")

val serverDirectory = assetDirectory.dir("bin")
val serverArchivesDirectory = layout.buildDirectory.dir("embedded-llama-server")

tasks.register<Delete>("cleanEmbeddedLlamaModel") {
    group = "model management"
    description = "Deletes the locally downloaded embedded GGUF models."
    delete(assetDirectory.dir("models"))
}

tasks.register<Delete>("cleanEmbeddedLlamaServer") {
    group = "model management"
    description = "Deletes the locally installed embedded llama server and cached server archives."
    delete(serverDirectory)
    delete(serverArchivesDirectory)
}
