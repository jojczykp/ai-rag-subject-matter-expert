package org.alterbit.aisme.chatmodel

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme")
data class ConfiguredChatModelsProperties(
    val chatModels: List<ConfiguredChatModelProperties> = listOf(
        ConfiguredChatModelProperties(
            id = "local-ollama-llama",
            displayName = "Local Ollama Llama",
            runtime = ChatModelRuntime.OLLAMA,
            mode = ChatModelMode.LOCAL_SERVER,
            availableOffline = false,
            baseUrl = "http://localhost:11434",
            modelName = "llama3.2",
        ),
    ),
)

data class ConfiguredChatModelProperties(
    val id: String,
    val displayName: String,
    val runtime: ChatModelRuntime,
    val mode: ChatModelMode,
    val availableOffline: Boolean,
    val baseUrl: String? = null,
    val modelName: String? = null,
) {
    init {
        require(id.isNotBlank()) { "aisme.chat-models.id must not be blank" }
        require(displayName.isNotBlank()) { "aisme.chat-models.display-name must not be blank" }
        require(baseUrl == null || baseUrl.isNotBlank()) { "aisme.chat-models.base-url must not be blank when configured" }
        require(modelName == null || modelName.isNotBlank()) { "aisme.chat-models.model-name must not be blank when configured" }
    }

    fun toDescriptor(): ChatModelDescriptor =
        ChatModelDescriptor(
            id = id,
            displayName = displayName,
            runtime = runtime,
            mode = mode,
            availableOffline = availableOffline,
            availability = ChatModelAvailability.CONFIGURED,
            baseUrl = baseUrl,
            modelName = modelName,
        )
}
