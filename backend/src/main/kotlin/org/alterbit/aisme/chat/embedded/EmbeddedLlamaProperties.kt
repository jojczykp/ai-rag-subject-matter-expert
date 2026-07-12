package org.alterbit.aisme.chat.embedded

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.embedded-llama")
data class EmbeddedLlamaProperties(
    val assetDirectory: String,
    val serverExecutablePath: String,
    val models: List<EmbeddedLlamaModelProperties>,
) {
    init {
        require(assetDirectory.isNotBlank()) { "aisme.embedded-llama.asset-directory must not be blank" }
        require(serverExecutablePath.isNotBlank()) {
            "aisme.embedded-llama.server-executable-path must not be blank"
        }
        require(models.isNotEmpty()) { "aisme.embedded-llama.models must not be empty" }
        require(models.map { it.id }.distinct().size == models.size) {
            "aisme.embedded-llama.models must not contain duplicate ids"
        }
    }

    fun enabledModels(): List<EmbeddedLlamaModelProperties> =
        models.filter { it.enabled }
}

data class EmbeddedLlamaModelProperties(
    val id: String,
    val enabled: Boolean = false,
    val displayName: String,
    val ggufFile: String,
    val contextSize: Int,
    val runtimeArguments: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "aisme.embedded-llama.models.id must not be blank" }
        require(displayName.isNotBlank()) { "aisme.embedded-llama.models.display-name must not be blank" }
        require(ggufFile.isNotBlank()) { "aisme.embedded-llama.models.gguf-file must not be blank" }
        require(contextSize > 0) { "aisme.embedded-llama.models.context-size must be greater than 0" }
        require(runtimeArguments.none { it.isBlank() }) {
            "aisme.embedded-llama.models.runtime-arguments must not contain blank values"
        }
    }
}
