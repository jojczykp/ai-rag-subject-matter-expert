package org.alterbit.aisme.model

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme")
data class ConfiguredModelsProperties(
    val models: List<ConfiguredModelProperties> = listOf(
        ConfiguredModelProperties(
            id = "local-ollama-llama",
            displayName = "Local Ollama Llama",
            runtime = ModelRuntime.OLLAMA,
            mode = ModelMode.LOCAL_SERVER,
            availableOffline = false,
            baseUrl = "http://localhost:11434",
        ),
    ),
)

data class ConfiguredModelProperties(
    val id: String,
    val displayName: String,
    val runtime: ModelRuntime,
    val mode: ModelMode,
    val availableOffline: Boolean,
    val baseUrl: String? = null,
) {
    init {
        require(id.isNotBlank()) { "aisme.models.id must not be blank" }
        require(displayName.isNotBlank()) { "aisme.models.display-name must not be blank" }
        require(baseUrl == null || baseUrl.isNotBlank()) { "aisme.models.base-url must not be blank when configured" }
    }

    fun toDescriptor(): ModelDescriptor =
        ModelDescriptor(
            id = id,
            displayName = displayName,
            runtime = runtime,
            mode = mode,
            availableOffline = availableOffline,
            availability = ModelAvailability.CONFIGURED,
            baseUrl = baseUrl,
        )
}
