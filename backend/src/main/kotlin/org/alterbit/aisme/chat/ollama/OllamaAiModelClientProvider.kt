package org.alterbit.aisme.chat.ollama

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
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
                chatApi = ollamaChatApiFactory.create(model.requireBaseUrl(), chatProperties.timeout),
            )
        }

    override fun clients(): List<AiModelClient> =
        clients
}
