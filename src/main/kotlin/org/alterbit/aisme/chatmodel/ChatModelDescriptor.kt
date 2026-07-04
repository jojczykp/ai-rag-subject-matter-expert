package org.alterbit.aisme.chatmodel

data class ChatModelDescriptor(
    val id: String,
    val displayName: String,
    val runtime: ChatModelRuntime,
    val mode: ChatModelMode,
    val availableOffline: Boolean,
    val availability: ChatModelAvailability,
    val baseUrl: String?,
    val modelName: String?,
    val apiKey: String?,
) {
    init {
        require(id.isNotBlank()) { "model id must not be blank" }
        require(displayName.isNotBlank()) { "model display name must not be blank" }
        require(baseUrl == null || baseUrl.isNotBlank()) { "model baseUrl must not be blank when configured" }
        require(modelName == null || modelName.isNotBlank()) { "model modelName must not be blank when configured" }
        require(apiKey == null || apiKey.isNotBlank()) { "model apiKey must not be blank when configured" }
    }
}
