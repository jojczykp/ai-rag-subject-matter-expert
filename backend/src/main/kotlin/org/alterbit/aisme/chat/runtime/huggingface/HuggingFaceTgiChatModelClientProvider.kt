package org.alterbit.aisme.chat.runtime.huggingface

import org.alterbit.aisme.chat.ChatModelClient
import org.alterbit.aisme.chat.ChatModelClientProvider
import org.alterbit.aisme.chat.catalog.ChatProperties
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HuggingFaceTgiChatModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    huggingFaceTgiChatApiFactory: HuggingFaceTgiChatApiFactory,
) : ChatModelClientProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val clients: List<ChatModelClient> = chatModelRegistry.chatModels()
        .filter { it.runtime == ChatModelRuntime.HUGGING_FACE_TGI }
        .map { model ->
            logger.info("Creating Hugging Face TGI AI model client for model '{}'", model.id)
            HuggingFaceTgiChatModelClient(
                model = model,
                chatApi = huggingFaceTgiChatApiFactory.create(
                    baseUrl = model.requireHuggingFaceTgiBaseUrl(),
                    apiKey = model.apiKey,
                    apiTimeout = chatProperties.apiTimeout,
                ),
            )
        }

    override fun clients(): List<ChatModelClient> =
        clients
}
