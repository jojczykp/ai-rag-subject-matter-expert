package org.alterbit.aisme.chat.embedded

import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.AiModelClientProvider
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ChatModelRuntime
import org.springframework.stereotype.Component

@Component
class LlamaRuntimeAiModelClientProvider(
    chatModelRegistry: ChatModelRegistry,
    chatProperties: ChatProperties,
    llamaRuntimeProperties: LlamaRuntimeProperties,
    llamaServerChatApiFactory: LlamaServerChatApiFactory,
) : AiModelClientProvider {
    private val clients: List<AiModelClient> = if (llamaRuntimeProperties.enabled) {
        val config = llamaRuntimeProperties.requireEnabledConfig()
        val runtimeModelsById = config.models.associateBy { it.id }
        val chatApi = llamaServerChatApiFactory.create(
            baseUrl = config.baseUrl(),
            timeout = chatProperties.timeout,
        )

        chatModelRegistry.chatModels()
            .filter { it.runtime == ChatModelRuntime.EMBEDDED_OFFLINE }
            .mapNotNull { model ->
                runtimeModelsById[model.id]?.let { runtimeModel ->
                    LlamaRuntimeAiModelClient(
                        model = model,
                        runtimeModel = runtimeModel,
                        chatApi = chatApi,
                    )
                }
            }
    } else {
        emptyList()
    }

    override fun clients(): List<AiModelClient> =
        clients

    private fun EnabledLlamaRuntimeProperties.baseUrl(): String =
        "http://$LOOPBACK_HOST:$port"

    private companion object {
        const val LOOPBACK_HOST = "127.0.0.1"
    }
}
