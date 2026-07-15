package org.alterbit.aisme.chat.catalog

data class ChatModelDescriptor(
    val id: String,
    val displayName: String,
    val description: String?,
    val runtime: ChatModelRuntime,
    val mode: ChatModelMode,
    val availableOffline: Boolean,
    val availability: ChatModelAvailability,
    val baseUrl: String?,
    val modelName: String?,
    val apiKey: String?,
    val displayOrder: Int? = null,
    val runtimeId: String = runtime.name,
    val assetDirectory: String? = null,
    val serverExecutablePath: String? = null,
    val ggufFile: String? = null,
    val contextSize: Int? = null,
    val runtimeArguments: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "model id must not be blank" }
        require(displayOrder == null || displayOrder >= 0) {
            "model displayOrder must not be negative when configured"
        }
        require(displayName.isNotBlank()) { "model display name must not be blank" }
        require(runtimeId.isNotBlank()) { "model runtimeId must not be blank" }
        require(description == null || description.isNotBlank()) {
            "model description must not be blank when configured"
        }
        require(baseUrl == null || baseUrl.isNotBlank()) { "model baseUrl must not be blank when configured" }
        require(modelName == null || modelName.isNotBlank()) { "model modelName must not be blank when configured" }
        require(apiKey == null || apiKey.isNotBlank()) { "model apiKey must not be blank when configured" }
        require(assetDirectory == null || assetDirectory.isNotBlank()) {
            "model assetDirectory must not be blank when configured"
        }
        require(serverExecutablePath == null || serverExecutablePath.isNotBlank()) {
            "model serverExecutablePath must not be blank when configured"
        }
        require(ggufFile == null || ggufFile.isNotBlank()) { "model ggufFile must not be blank when configured" }
        require(contextSize == null || contextSize > 0) {
            "model contextSize must be greater than 0 when configured"
        }
        require(runtimeArguments.none { it.isBlank() }) {
            "model runtimeArguments must not contain blank values"
        }
    }
}
