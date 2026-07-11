package org.alterbit.aisme.chat.ollama

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.springframework.stereotype.Component

@Component
class OllamaAiModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    ollamaChatApiFactory: OllamaChatApiFactory,
) : AiModelClientProvider {
    private val clients: List<AiModelClient> = chatModelRegistry.chatModels()
        .filter { it.runtime == ChatModelRuntime.OLLAMA }
        .map { model ->
            OllamaAiModelClient(
                model = model,
                chatApi = ollamaChatApiFactory.create(model.requireBaseUrl(), chatProperties.timeout),
            )
        }

    override fun clients(): List<AiModelClient> =
        clients
}
