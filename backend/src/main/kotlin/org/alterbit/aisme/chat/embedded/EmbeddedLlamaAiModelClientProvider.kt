package org.alterbit.aisme.chat.embedded

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
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
            .filter { it.runtime == ChatModelRuntime.EMBEDDED_OFFLINE }
            .mapNotNull { model ->
                val baseUrl = embeddedLlamaProcessManager.baseUrlForModelId(model.id)
                if (baseUrl != null) {
                    logger.info("Creating embedded llama AI model client for model '{}'", model.id)
                    EmbeddedLlamaAiModelClient(
                        model = model,
                        chatApi = llamaServerChatApiFactory.create(
                            baseUrl = baseUrl,
                            timeout = chatProperties.timeout,
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
