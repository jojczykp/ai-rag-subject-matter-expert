package org.alterbit.aisme.chat.huggingface

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HuggingFaceTgiAiModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    huggingFaceTgiChatApiFactory: HuggingFaceTgiChatApiFactory,
) : AiModelClientProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val clients: List<AiModelClient> = chatModelRegistry.chatModels()
        .filter { it.runtime == ChatModelRuntime.HUGGING_FACE_TGI }
        .map { model ->
            logger.info("Creating Hugging Face TGI AI model client for model '{}'", model.id)
            HuggingFaceTgiAiModelClient(
                model = model,
                chatApi = huggingFaceTgiChatApiFactory.create(
                    baseUrl = model.requireHuggingFaceTgiBaseUrl(),
                    apiKey = model.apiKey,
                    apiTimeout = chatProperties.apiTimeout,
                ),
            )
        }

    override fun clients(): List<AiModelClient> =
        clients
}
