package org.alterbit.aisme.chat.embedded

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.embedded-llama")
data class EmbeddedLlamaProperties(
    val enabled: Boolean = false,
    val config: EnabledEmbeddedLlamaProperties? = null,
) {
    init {
        require(!enabled || config != null) {
            "aisme.embedded-llama.config is required when aisme.embedded-llama.enabled is true"
        }
    }

    fun requireEnabledConfig(): EnabledEmbeddedLlamaProperties {
        require(enabled) { "aisme.embedded-llama.enabled must be true" }
        return checkNotNull(config) {
            "aisme.embedded-llama.config is required when aisme.embedded-llama.enabled is true"
        }
    }
}

data class EnabledEmbeddedLlamaProperties(
    val assetDirectory: String,
    val serverExecutablePath: String,
    val models: List<EmbeddedLlamaModelProperties>,
) {
    init {
        require(assetDirectory.isNotBlank()) { "aisme.embedded-llama.config.asset-directory must not be blank" }
        require(serverExecutablePath.isNotBlank()) {
            "aisme.embedded-llama.config.server-executable-path must not be blank"
        }
        require(models.isNotEmpty()) { "aisme.embedded-llama.config.models must not be empty" }
        require(models.map { it.id }.distinct().size == models.size) {
            "aisme.embedded-llama.config.models must not contain duplicate ids"
        }
    }
}

data class EmbeddedLlamaModelProperties(
    val id: String,
    val displayName: String,
    val ggufFile: String,
    val contextSize: Int,
    val runtimeArguments: List<String> = emptyList(),
    val sha256: String? = null,
) {
    init {
        require(id.isNotBlank()) { "aisme.embedded-llama.config.models.id must not be blank" }
        require(displayName.isNotBlank()) { "aisme.embedded-llama.config.models.display-name must not be blank" }
        require(ggufFile.isNotBlank()) { "aisme.embedded-llama.config.models.gguf-file must not be blank" }
        require(contextSize > 0) { "aisme.embedded-llama.config.models.context-size must be greater than 0" }
        require(runtimeArguments.none { it.isBlank() }) {
            "aisme.embedded-llama.config.models.runtime-arguments must not contain blank values"
        }
        require(sha256 == null || sha256.matches(SHA_256_PATTERN)) {
            "aisme.embedded-llama.config.models.sha256 must be a 64-character lowercase hexadecimal value"
        }
    }

    private companion object {
        private val SHA_256_PATTERN = Regex("[a-f0-9]{64}")
    }
}
