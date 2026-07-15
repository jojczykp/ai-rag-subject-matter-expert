package org.alterbit.aisme.chat.runtime.embedded

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmbeddedLlamaAiModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    embeddedLlamaProcessManager: EmbeddedLlamaProcessManager,
    llamaServerChatApiFactory: LlamaServerChatApiFactory,
) : AiModelClientProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val clients: List<AiModelClient> = run {
        chatModelRegistry.chatModels()
            .filter { it.runtime == ChatModelRuntime.EMBEDDED_LLAMA }
            .mapNotNull { model ->
                val baseUrl = embeddedLlamaProcessManager.baseUrlForModelId(model.id)
                if (baseUrl != null) {
                    logger.info("Creating embedded llama AI model client for model '{}'", model.id)
                    EmbeddedLlamaAiModelClient(
                        model = model,
                        chatApi = llamaServerChatApiFactory.create(
                            baseUrl = baseUrl,
                            apiTimeout = chatProperties.apiTimeout,
                        ),
                    )
                } else {
                    logger.warn(
                        "Skipping embedded llama AI model client for model '{}' because runtime is not ready",
                        model.id,
                    )
                    null
                }
            }
    }

    override fun clients(): List<AiModelClient> =
        clients
}
