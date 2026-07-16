package org.alterbit.aisme.chat.runtime.ollama

import org.alterbit.aisme.chat.ChatModelClient
import org.alterbit.aisme.chat.ChatModelClientProvider
import org.alterbit.aisme.chat.catalog.ChatProperties
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OllamaChatModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    ollamaChatApiFactory: OllamaChatApiFactory,
) : ChatModelClientProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val clients: List<ChatModelClient> = chatModelRegistry.chatModels()
        .filter { it.runtime == ChatModelRuntime.OLLAMA }
        .map { model ->
            logger.info("Creating Ollama AI model client for model '{}'", model.id)
            OllamaChatModelClient(
                model = model,
                chatApi = ollamaChatApiFactory.create(model.requireBaseUrl(), chatProperties.apiTimeout),
            )
        }

    override fun clients(): List<ChatModelClient> =
        clients
}
