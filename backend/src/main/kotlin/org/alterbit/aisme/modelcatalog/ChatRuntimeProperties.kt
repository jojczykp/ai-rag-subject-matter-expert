package org.alterbit.aisme.modelcatalog

data class ChatRuntimeProperties(
    val type: ChatModelRuntime,
    val baseUrl: String? = null,
    val apiKey: String? = null,
    val assetDirectory: String? = null,
    val serverExecutablePath: String? = null,
) {
    init {
        require(baseUrl == null || baseUrl.isNotBlank()) {
            "aisme.runtimes.base-url must not be blank when configured"
        }
        require(assetDirectory == null || assetDirectory.isNotBlank()) {
            "aisme.runtimes.asset-directory must not be blank when configured"
        }
        require(serverExecutablePath == null || serverExecutablePath.isNotBlank()) {
            "aisme.runtimes.server-executable-path must not be blank when configured"
        }
    }

    val mode: ChatModelMode =
        when (type) {
            ChatModelRuntime.OLLAMA -> ChatModelMode.LOCAL_SERVER
            ChatModelRuntime.OPENAI_COMPATIBLE,
            ChatModelRuntime.HUGGING_FACE_TGI,
            ChatModelRuntime.SPRING_AI,
            -> ChatModelMode.ONLINE
            ChatModelRuntime.EMBEDDED_LLAMA -> ChatModelMode.EMBEDDED_OFFLINE
        }

    val availableOffline: Boolean =
        type == ChatModelRuntime.EMBEDDED_LLAMA
}
