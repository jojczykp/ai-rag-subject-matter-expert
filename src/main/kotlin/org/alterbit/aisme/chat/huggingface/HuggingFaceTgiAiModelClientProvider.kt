package org.alterbit.aisme.chat.huggingface

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.springframework.stereotype.Component

@Component
class HuggingFaceTgiAiModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    huggingFaceTgiChatApiFactory: HuggingFaceTgiChatApiFactory,
) : AiModelClientProvider {
    private val clients: List<AiModelClient> = chatModelRegistry.chatModels()
        .filter { it.runtime == ChatModelRuntime.HUGGING_FACE_ENDPOINT }
        .map { model ->
            HuggingFaceTgiAiModelClient(
                model = model,
                chatApi = huggingFaceTgiChatApiFactory.create(
                    baseUrl = model.requireHuggingFaceTgiBaseUrl(),
                    apiKey = model.apiKey,
                    timeout = chatProperties.timeout,
                ),
            )
        }

    override fun clients(): List<AiModelClient> =
        clients
}
