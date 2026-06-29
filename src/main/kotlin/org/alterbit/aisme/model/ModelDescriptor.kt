package org.alterbit.aisme.model

data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val runtime: ModelRuntime,
    val mode: ModelMode,
    val availableOffline: Boolean,
    val availability: ModelAvailability,
    val baseUrl: String?,
) {
    init {
        require(id.isNotBlank()) { "model id must not be blank" }
        require(displayName.isNotBlank()) { "model display name must not be blank" }
        require(baseUrl == null || baseUrl.isNotBlank()) { "model baseUrl must not be blank when configured" }
    }
}
