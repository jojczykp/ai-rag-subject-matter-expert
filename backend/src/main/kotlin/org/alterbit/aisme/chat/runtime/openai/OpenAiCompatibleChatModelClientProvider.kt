package org.alterbit.aisme.chat.runtime.openai

import org.alterbit.aisme.chat.ChatModelClient
import org.alterbit.aisme.chat.ChatModelClientProvider
import org.alterbit.aisme.chat.catalog.ChatProperties
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OpenAiCompatibleChatModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    openAiCompatibleChatApiFactory: OpenAiCompatibleChatApiFactory,
) : ChatModelClientProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val clients: List<ChatModelClient> = chatModelRegistry.chatModels()
        .filter { it.runtime == ChatModelRuntime.OPENAI_COMPATIBLE }
        .mapNotNull { model ->
            val apiKey = model.apiKey?.takeIf(String::isNotBlank)
            if (apiKey == null) {
                logger.warn("Skipping OpenAI-compatible client for model '{}' because api key is missing", model.id)
                return@mapNotNull null
            }

            logger.info("Creating OpenAI-compatible AI model client for model '{}'", model.id)
            OpenAiCompatibleChatModelClient(
                model = model,
                chatApi = openAiCompatibleChatApiFactory.create(
                    baseUrl = model.requireOpenAiCompatibleBaseUrl(),
                    apiKey = apiKey,
                    apiTimeout = chatProperties.apiTimeout,
                ),
            )
        }

    override fun clients(): List<ChatModelClient> =
        clients
}
