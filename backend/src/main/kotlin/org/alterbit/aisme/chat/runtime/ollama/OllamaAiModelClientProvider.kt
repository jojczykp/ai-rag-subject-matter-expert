package org.alterbit.aisme.chat.runtime.ollama

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.catalog.ChatProperties
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OllamaAiModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    ollamaChatApiFactory: OllamaChatApiFactory,
) : AiModelClientProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val clients: List<AiModelClient> = chatModelRegistry.chatModels()
        .filter { it.runtime == ChatModelRuntime.OLLAMA }
        .map { model ->
            logger.info("Creating Ollama AI model client for model '{}'", model.id)
            OllamaAiModelClient(
                model = model,
                chatApi = ollamaChatApiFactory.create(model.requireBaseUrl(), chatProperties.apiTimeout),
            )
        }

    override fun clients(): List<AiModelClient> =
        clients
}
