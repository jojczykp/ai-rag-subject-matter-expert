package org.alterbit.aisme.modelcatalog

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ChatModelRegistry(
    properties: ConfiguredChatModelsProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val runtimesById: Map<String, ChatRuntimeProperties> = properties.runtimes
        .also { runtimes ->
            require(runtimes.isNotEmpty()) { "aisme.runtimes must contain at least one runtime" }
            require(runtimes.keys.none { it.isBlank() }) { "aisme.runtimes must not contain blank ids" }
        }

    private val modelsById: Map<String, ChatModelDescriptor> = properties.chatModelsById
        .also { modelsById ->
            require(modelsById.isNotEmpty()) { "aisme.chat-models must contain at least one model" }
            require(modelsById.keys.none { it.isBlank() }) { "aisme.chat-models must not contain blank ids" }
        }
        .entries
        .map { entry ->
            IndexedChatModelProperties(
                id = entry.key,
                model = entry.value,
            )
        }
        .filter { it.model.enabled }
        .onEach { it.validateRuntimeConfiguration() }
        .map { it.model.toDescriptor(id = it.id, configuredRuntime = runtimesById.getValue(it.model.requireRuntimeId())) }
        .also { models ->
            require(models.isNotEmpty()) { "aisme.chat-models must contain at least one model" }
        }
        .sortedWith(compareBy<ChatModelDescriptor> { it.displayOrder ?: Int.MAX_VALUE }.thenBy { it.id })
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

    private fun IndexedChatModelProperties.validateRuntimeConfiguration() {
        val modelPrefix = "aisme.chat-models.$id"
        val runtimeId = model.requireRuntimeId()
        val runtime = runtimesById[runtimeId]
        require(runtime != null) { "$modelPrefix.runtime.id references unknown runtime '$runtimeId'" }
        val descriptor = model.toDescriptor(id = id, configuredRuntime = runtime)

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
                requireConfigured(model.runtime.modelName, "runtime.model-name")
            }

            ChatModelRuntime.OPENAI_COMPATIBLE -> {
                requireConfigured(descriptor.baseUrl, "base-url")
                requireConfigured(model.runtime.modelName, "runtime.model-name")
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
                requireConfigured(model.runtime.modelName, "runtime.model-name")
                requireConfigured(model.runtime.ggufFile, "runtime.gguf-file")
                requireConfigured(model.runtime.contextSize?.toString(), "runtime.context-size")
            }
        }
    }

    private data class IndexedChatModelProperties(
        val id: String,
        val model: ChatModelProperties,
    )
}
