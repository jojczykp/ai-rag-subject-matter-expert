package org.alterbit.aisme.chat.catalog

import org.alterbit.aisme.assets.ModelAssetProperties

data class ChatModelProperties(
    val enabled: Boolean = false,
    val downloadMissingAssetsOnStartup: Boolean = true,
    val displayOrder: Int? = null,
    val displayName: String? = null,
    val description: String? = null,
    val assets: List<ModelAssetProperties> = emptyList(),
    val runtime: ChatModelRuntimeProperties = ChatModelRuntimeProperties(),
) {
    init {
        require(displayOrder == null || displayOrder >= 0) {
            "aisme.chat.models.display-order must not be negative when configured"
        }
        require(displayName == null || displayName.isNotBlank()) {
            "aisme.chat.models.display-name must not be blank"
        }
    }

    fun requireDisplayName(): String {
        require(enabled) { "aisme.chat.models.enabled must be true" }
        require(displayName != null) {
            "aisme.chat.models.display-name is required when aisme.chat.models.enabled is true"
        }
        return displayName
    }

    fun requireRuntimeId(): String {
        require(enabled) { "aisme.chat.models.enabled must be true" }
        require(runtime.id != null) {
            "aisme.chat.models.runtime.id is required when aisme.chat.models.enabled is true"
        }
        return runtime.id
    }

    fun toDescriptor(
        id: String,
        configuredRuntime: ChatModelRuntimeConfigProperties,
    ): ChatModelDescriptor {
        return ChatModelDescriptor(
            id = id,
            enabled = enabled,
            downloadMissingAssetsOnStartup = downloadMissingAssetsOnStartup,
            assets = assets.map { asset -> asset.toModelAsset(id) },
            displayOrder = displayOrder,
            displayName = requireDisplayName(),
            description = description.normalizedOptionalValue(),
            runtimeId = requireRuntimeId(),
            runtime = configuredRuntime.type,
            mode = configuredRuntime.mode,
            availableOffline = configuredRuntime.availableOffline,
            availability = ChatModelAvailability.CONFIGURED,
            baseUrl = configuredRuntime.baseUrl.normalizedOptionalValue(),
            modelName = runtime.modelName.normalizedOptionalValue(),
            apiKey = configuredRuntime.apiKey.normalizedOptionalValue(),
            assetDirectory = configuredRuntime.assetDirectory.normalizedOptionalValue(),
            serverExecutablePath = configuredRuntime.serverExecutablePath.normalizedOptionalValue(),
            ggufFile = runtime.ggufFile.normalizedOptionalValue(),
            contextSize = runtime.contextSize,
            runtimeArguments = runtime.runtimeArguments,
        )
    }

    private fun String?.normalizedOptionalValue(): String? =
        this?.trim()?.takeIf(String::isNotEmpty)
}
