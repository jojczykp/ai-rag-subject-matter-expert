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
    llamaRuntimeProcessManager: LlamaRuntimeProcessManager,
    llamaServerChatApiFactory: LlamaServerChatApiFactory,
) : AiModelClientProvider {
    private val clients: List<AiModelClient> = if (llamaRuntimeProperties.enabled) {
        val config = llamaRuntimeProperties.requireEnabledConfig()
        val runtimeModelsById = config.models.associateBy { it.id }

        chatModelRegistry.chatModels()
            .filter { it.runtime == ChatModelRuntime.EMBEDDED_OFFLINE }
            .mapNotNull { model ->
                val baseUrl = llamaRuntimeProcessManager.baseUrlForModelId(model.id)
                val runtimeModel = runtimeModelsById[model.id]
                if (baseUrl != null && runtimeModel != null) {
                    LlamaRuntimeAiModelClient(
                        model = model,
                        runtimeModel = runtimeModel,
                        chatApi = llamaServerChatApiFactory.create(
                            baseUrl = baseUrl,
                            timeout = chatProperties.timeout,
                        ),
                    )
                } else {
                    null
                }
            }
    } else {
        emptyList()
    }

    override fun clients(): List<AiModelClient> =
        clients
}
