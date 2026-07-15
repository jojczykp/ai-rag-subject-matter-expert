package org.alterbit.aisme.chat.catalog

data class ChatModelRuntimeProperties(
    val id: String? = null,
    val modelName: String? = null,
    val ggufFile: String? = null,
    val contextSize: Int? = null,
    val runtimeArguments: List<String> = emptyList(),
) {
    init {
        require(id == null || id.isNotBlank()) {
            "aisme.chat.models.runtime.id must not be blank"
        }
        require(modelName == null || modelName.isNotBlank()) {
            "aisme.chat.models.runtime.model-name must not be blank when configured"
        }
        require(ggufFile == null || ggufFile.isNotBlank()) {
            "aisme.chat.models.runtime.gguf-file must not be blank when configured"
        }
        require(contextSize == null || contextSize > 0) {
            "aisme.chat.models.runtime.context-size must be greater than 0 when configured"
        }
        require(runtimeArguments.none { it.isBlank() }) {
            "aisme.chat.models.runtime.runtime-arguments must not contain blank values"
        }
    }
}
