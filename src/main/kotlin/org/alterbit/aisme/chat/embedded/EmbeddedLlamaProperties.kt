package org.alterbit.aisme.chat.embedded

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.embedded-llama")
data class EmbeddedLlamaProperties(
    val assetDirectory: String = "./models/llama",
    val serverExecutablePath: String = "./bin/llama-server",
    val host: String = "127.0.0.1",
    val port: Int = 18080,
    val models: List<EmbeddedLlamaModelProperties> = listOf(
        EmbeddedLlamaModelProperties(
            id = "embedded-llama",
            displayName = "Embedded Llama",
            ggufFile = "models/llama.gguf",
            contextSize = 4096,
            runtimeArguments = emptyList(),
            sha256 = null,
            license = "TODO",
            hardwareRequirements = "TODO",
        ),
    ),
) {
    init {
        require(assetDirectory.isNotBlank()) { "aisme.embedded-llama.asset-directory must not be blank" }
        require(serverExecutablePath.isNotBlank()) { "aisme.embedded-llama.server-executable-path must not be blank" }
        require(host.isNotBlank()) { "aisme.embedded-llama.host must not be blank" }
        require(port in 1..65535) { "aisme.embedded-llama.port must be between 1 and 65535" }
        require(models.map { it.id }.distinct().size == models.size) {
            "aisme.embedded-llama.models must not contain duplicate ids"
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
    val license: String,
    val hardwareRequirements: String,
) {
    init {
        require(id.isNotBlank()) { "aisme.embedded-llama.models.id must not be blank" }
        require(displayName.isNotBlank()) { "aisme.embedded-llama.models.display-name must not be blank" }
        require(ggufFile.isNotBlank()) { "aisme.embedded-llama.models.gguf-file must not be blank" }
        require(contextSize > 0) { "aisme.embedded-llama.models.context-size must be greater than 0" }
        require(runtimeArguments.none { it.isBlank() }) {
            "aisme.embedded-llama.models.runtime-arguments must not contain blank values"
        }
        require(sha256 == null || sha256.matches(SHA_256_PATTERN)) {
            "aisme.embedded-llama.models.sha256 must be a 64-character lowercase hexadecimal value"
        }
        require(license.isNotBlank()) { "aisme.embedded-llama.models.license must not be blank" }
        require(hardwareRequirements.isNotBlank()) {
            "aisme.embedded-llama.models.hardware-requirements must not be blank"
        }
    }

    private companion object {
        private val SHA_256_PATTERN = Regex("[a-f0-9]{64}")
    }
}
