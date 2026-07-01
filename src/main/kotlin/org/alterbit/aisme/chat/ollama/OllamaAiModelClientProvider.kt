package org.alterbit.aisme.chat.ollama

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.springframework.stereotype.Component

@Component
class OllamaAiModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    ollamaChatApiFactory: OllamaChatApiFactory,
) : AiModelClientProvider {
    private val clients = chatModelRegistry.chatModels()
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
