package org.alterbit.aisme.modelcatalog

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ChatModelRegistry(
    properties: ConfiguredChatModelsProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val runtimesById: Map<String, ConfiguredChatRuntimeProperties> = properties.runtimes
        .also { runtimes ->
            require(runtimes.isNotEmpty()) { "aisme.runtimes must contain at least one runtime" }
            require(runtimes.keys.none { it.isBlank() }) { "aisme.runtimes must not contain blank ids" }
        }

    private val modelsById: Map<String, ChatModelDescriptor> = properties.chatModels
        .also { models ->
            require(models.map { it.id }.distinct().size == models.size) {
                "aisme.chat-models must not contain duplicate ids"
            }
        }
        .mapIndexed { index, model -> IndexedConfiguredChatModel(index = index, model = model) }
        .filter { it.model.enabled }
        .onEach { it.validateRuntimeConfiguration() }
        .map { it.model.toDescriptor(runtime = runtimesById.getValue(it.model.requireRuntimeId())) }
        .also { models ->
            require(models.isNotEmpty()) { "aisme.chat-models must contain at least one model" }
        }
        .associateBy { it.id }
        .also { models ->
            logger.info("Configured {} enabled chat model(s)", models.size)
            models.values.forEach { model ->
                logger.info(
                    "Chat model '{}' configured with runtime id '{}', type '{}', and mode '{}'",
                    model.id,
                    model.runtimeId,
                    model.runtime,
                    model.mode,
                )
            }
        }

    fun chatModels(): List<ChatModelDescriptor> =
        modelsById.values.toList()

    fun findById(modelId: String): ChatModelDescriptor? =
        modelsById[modelId]

    fun getByIdOrThrow(modelId: String): ChatModelDescriptor =
        findById(modelId) ?: throw ChatModelNotFoundException(modelId)

    private fun IndexedConfiguredChatModel.validateRuntimeConfiguration() {
        val modelPrefix = "aisme.chat-models[$index]"
        val runtimeId = model.requireRuntimeId()
        val runtime = runtimesById[runtimeId]
        require(runtime != null) { "$modelPrefix.runtime-id references unknown runtime '$runtimeId'" }
        val descriptor = model.toDescriptor(runtime)

        fun requireConfigured(value: String?, propertyName: String) {
            require(value != null) { "$modelPrefix.$propertyName is required for ${runtime.type} models" }
        }

        fun requireRuntimeConfigured(value: String?, propertyName: String) {
            require(value != null) {
                "aisme.runtimes.$runtimeId.$propertyName is required for ${runtime.type} runtimes"
            }
        }

        when (runtime.type) {
            ChatModelRuntime.OLLAMA -> {
                requireConfigured(descriptor.baseUrl, "base-url")
                requireConfigured(model.modelName, "model-name")
            }

            ChatModelRuntime.OPENAI_COMPATIBLE -> {
                requireConfigured(descriptor.baseUrl, "base-url")
                requireConfigured(model.modelName, "model-name")
            }

            ChatModelRuntime.HUGGING_FACE_ENDPOINT -> {
                requireConfigured(descriptor.baseUrl, "base-url")
            }

            ChatModelRuntime.SPRING_AI -> {
                // Spring AI models rely on Spring AI auto-configuration.
            }

            ChatModelRuntime.EMBEDDED_OFFLINE -> {
                requireRuntimeConfigured(runtime.assetDirectory, "asset-directory")
                requireRuntimeConfigured(runtime.serverExecutablePath, "server-executable-path")
                requireConfigured(model.modelName, "model-name")
                requireConfigured(model.ggufFile, "gguf-file")
                requireConfigured(model.contextSize?.toString(), "context-size")
            }
        }
    }

    private data class IndexedConfiguredChatModel(
        val index: Int,
        val model: ConfiguredChatModelProperties,
    )
}
