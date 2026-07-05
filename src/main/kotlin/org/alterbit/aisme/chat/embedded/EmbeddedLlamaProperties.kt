package org.alterbit.aisme.chat.embedded

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.embedded-llama")
data class EmbeddedLlamaProperties(
    val assetDirectory: String = "./models/llama",
    val serverExecutablePath: String = "./bin/llama-server",
    val host: String = "127.0.0.1",
    val port: Int = 18080,
) {
    init {
        require(assetDirectory.isNotBlank()) { "aisme.embedded-llama.asset-directory must not be blank" }
        require(serverExecutablePath.isNotBlank()) { "aisme.embedded-llama.server-executable-path must not be blank" }
        require(host.isNotBlank()) { "aisme.embedded-llama.host must not be blank" }
        require(port in 1..65535) { "aisme.embedded-llama.port must be between 1 and 65535" }
    }
}
