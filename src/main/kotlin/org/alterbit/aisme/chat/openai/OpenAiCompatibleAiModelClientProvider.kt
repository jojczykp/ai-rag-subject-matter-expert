package org.alterbit.aisme.chat.openai

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OpenAiCompatibleAiModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    openAiCompatibleChatApiFactory: OpenAiCompatibleChatApiFactory,
) : AiModelClientProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val clients: List<AiModelClient> = chatModelRegistry.chatModels()
        .filter { it.runtime == ChatModelRuntime.OPENAI_COMPATIBLE }
        .mapNotNull { model ->
            val apiKey = model.apiKey?.takeIf(String::isNotBlank)
            if (apiKey == null) {
                logger.warn("Skipping OpenAI-compatible client for model '{}' because api key is missing", model.id)
                return@mapNotNull null
            }

            logger.info("Creating OpenAI-compatible AI model client for model '{}'", model.id)
            OpenAiCompatibleAiModelClient(
                model = model,
                chatApi = openAiCompatibleChatApiFactory.create(
                    baseUrl = model.requireOpenAiCompatibleBaseUrl(),
                    apiKey = apiKey,
                    timeout = chatProperties.timeout,
                ),
            )
        }

    override fun clients(): List<AiModelClient> =
        clients
}
