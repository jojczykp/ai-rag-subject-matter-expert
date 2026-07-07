package org.alterbit.aisme.chat.embedded

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.llama-runtime")
data class LlamaRuntimeProperties(
    val enabled: Boolean = false,
    val config: EnabledLlamaRuntimeProperties? = null,
) {
    init {
        require(!enabled || config != null) {
            "aisme.llama-runtime.config is required when aisme.llama-runtime.enabled is true"
        }
    }

    fun requireEnabledConfig(): EnabledLlamaRuntimeProperties {
        require(enabled) { "aisme.llama-runtime.enabled must be true" }
        return checkNotNull(config) {
            "aisme.llama-runtime.config is required when aisme.llama-runtime.enabled is true"
        }
    }
}

data class EnabledLlamaRuntimeProperties(
    val assetDirectory: String,
    val serverExecutablePath: String,
    val models: List<LlamaRuntimeModelProperties>,
) {
    init {
        require(assetDirectory.isNotBlank()) { "aisme.llama-runtime.config.asset-directory must not be blank" }
        require(serverExecutablePath.isNotBlank()) {
            "aisme.llama-runtime.config.server-executable-path must not be blank"
        }
        require(models.isNotEmpty()) { "aisme.llama-runtime.config.models must not be empty" }
        require(models.map { it.id }.distinct().size == models.size) {
            "aisme.llama-runtime.config.models must not contain duplicate ids"
        }
    }
}

data class LlamaRuntimeModelProperties(
    val id: String,
    val displayName: String,
    val ggufFile: String,
    val contextSize: Int,
    val runtimeArguments: List<String> = emptyList(),
    val sha256: String? = null,
    val license: String,
    val hardwareRequirements: String,
) {
    init {
        require(id.isNotBlank()) { "aisme.llama-runtime.config.models.id must not be blank" }
        require(displayName.isNotBlank()) { "aisme.llama-runtime.config.models.display-name must not be blank" }
        require(ggufFile.isNotBlank()) { "aisme.llama-runtime.config.models.gguf-file must not be blank" }
        require(contextSize > 0) { "aisme.llama-runtime.config.models.context-size must be greater than 0" }
        require(runtimeArguments.none { it.isBlank() }) {
            "aisme.llama-runtime.config.models.runtime-arguments must not contain blank values"
        }
        require(sha256 == null || sha256.matches(SHA_256_PATTERN)) {
            "aisme.llama-runtime.config.models.sha256 must be a 64-character lowercase hexadecimal value"
        }
        require(license.isNotBlank()) { "aisme.llama-runtime.config.models.license must not be blank" }
        require(hardwareRequirements.isNotBlank()) {
            "aisme.llama-runtime.config.models.hardware-requirements must not be blank"
        }
    }

    private companion object {
        private val SHA_256_PATTERN = Regex("[a-f0-9]{64}")
    }
}
