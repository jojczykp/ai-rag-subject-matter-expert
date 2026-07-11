package org.alterbit.aisme.modelcatalog

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme")
data class ConfiguredChatModelsProperties(
    val chatModels: List<ConfiguredChatModelProperties> = listOf(
        ConfiguredChatModelProperties(
            id = "local-ollama-llama",
            enabled = true,
            config = EnabledChatModelProperties(
                displayName = "Local Ollama Llama",
                description = "Local Ollama model for chat requests when Ollama is running on this machine.",
                runtime = ChatModelRuntime.OLLAMA,
                mode = ChatModelMode.LOCAL_SERVER,
                availableOffline = false,
                baseUrl = "http://localhost:11434",
                modelName = "llama3.2",
            ),
        ),
    ),
)

data class ConfiguredChatModelProperties(
    val id: String,
    val enabled: Boolean = false,
    val config: EnabledChatModelProperties? = null,
) {
    init {
        require(id.isNotBlank()) { "aisme.chat-models.id must not be blank" }
        require(!enabled || config != null) {
            "aisme.chat-models.config is required when aisme.chat-models.enabled is true"
        }
    }

    fun requireEnabledConfig(): EnabledChatModelProperties {
        require(enabled) { "aisme.chat-models.enabled must be true" }
        return checkNotNull(config) {
            "aisme.chat-models.config is required when aisme.chat-models.enabled is true"
        }
    }

    fun toDescriptor(): ChatModelDescriptor {
        val config = requireEnabledConfig()
        return ChatModelDescriptor(
            id = id,
            displayName = config.displayName,
            description = config.description.normalizedOptionalValue(),
            runtime = config.runtime,
            mode = config.mode,
            availableOffline = config.availableOffline,
            availability = ChatModelAvailability.CONFIGURED,
            baseUrl = config.baseUrl.normalizedOptionalValue(),
            modelName = config.modelName.normalizedOptionalValue(),
            apiKey = config.apiKey.normalizedOptionalValue(),
        )
    }

    private fun String?.normalizedOptionalValue(): String? =
        this?.trim()?.takeIf(String::isNotEmpty)
}

data class EnabledChatModelProperties(
    val displayName: String,
    val description: String? = null,
    val runtime: ChatModelRuntime,
    val mode: ChatModelMode,
    val availableOffline: Boolean,
    val baseUrl: String? = null,
    val modelName: String? = null,
    val apiKey: String? = null,
) {
    init {
        require(displayName.isNotBlank()) {
            "aisme.chat-models.config.display-name must not be blank"
        }
        require(baseUrl == null || baseUrl.isNotBlank()) {
            "aisme.chat-models.config.base-url must not be blank when configured"
        }
        require(modelName == null || modelName.isNotBlank()) {
            "aisme.chat-models.config.model-name must not be blank when configured"
        }
    }
}
