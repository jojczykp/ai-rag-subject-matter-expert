package org.alterbit.aisme.modelcatalog

data class ConfiguredChatModelProperties(
    val enabled: Boolean = false,
    val displayOrder: Int? = null,
    val displayName: String? = null,
    val description: String? = null,
    val runtimeId: String? = null,
    val modelName: String? = null,
    val ggufFile: String? = null,
    val contextSize: Int? = null,
    val runtimeArguments: List<String> = emptyList(),
) {
    init {
        require(displayOrder == null || displayOrder >= 0) {
            "aisme.chat-models.display-order must not be negative when configured"
        }
        require(displayName == null || displayName.isNotBlank()) {
            "aisme.chat-models.display-name must not be blank"
        }
        require(runtimeId == null || runtimeId.isNotBlank()) {
            "aisme.chat-models.runtime-id must not be blank"
        }
        require(modelName == null || modelName.isNotBlank()) {
            "aisme.chat-models.model-name must not be blank when configured"
        }
        require(ggufFile == null || ggufFile.isNotBlank()) {
            "aisme.chat-models.gguf-file must not be blank when configured"
        }
        require(contextSize == null || contextSize > 0) {
            "aisme.chat-models.context-size must be greater than 0 when configured"
        }
        require(runtimeArguments.none { it.isBlank() }) {
            "aisme.chat-models.runtime-arguments must not contain blank values"
        }
    }

    fun requireDisplayName(): String {
        require(enabled) { "aisme.chat-models.enabled must be true" }
        require(displayName != null) {
            "aisme.chat-models.display-name is required when aisme.chat-models.enabled is true"
        }
        return displayName
    }

    fun requireRuntimeId(): String {
        require(enabled) { "aisme.chat-models.enabled must be true" }
        require(runtimeId != null) {
            "aisme.chat-models.runtime-id is required when aisme.chat-models.enabled is true"
        }
        return runtimeId
    }

    fun toDescriptor(
        id: String,
        runtime: ConfiguredChatRuntimeProperties,
    ): ChatModelDescriptor {
        return ChatModelDescriptor(
            id = id,
            displayOrder = displayOrder,
            displayName = requireDisplayName(),
            description = description.normalizedOptionalValue(),
            runtimeId = requireRuntimeId(),
            runtime = runtime.type,
            mode = runtime.mode,
            availableOffline = runtime.availableOffline,
            availability = ChatModelAvailability.CONFIGURED,
            baseUrl = runtime.baseUrl.normalizedOptionalValue(),
            modelName = modelName.normalizedOptionalValue(),
            apiKey = runtime.apiKey.normalizedOptionalValue(),
            assetDirectory = runtime.assetDirectory.normalizedOptionalValue(),
            serverExecutablePath = runtime.serverExecutablePath.normalizedOptionalValue(),
            ggufFile = ggufFile.normalizedOptionalValue(),
            contextSize = contextSize,
            runtimeArguments = runtimeArguments,
        )
    }

    private fun String?.normalizedOptionalValue(): String? =
        this?.trim()?.takeIf(String::isNotEmpty)
}
