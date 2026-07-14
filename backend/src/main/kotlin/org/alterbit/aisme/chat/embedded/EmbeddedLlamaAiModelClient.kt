package org.alterbit.aisme.chat.embedded

import org.alterbit.aisme.chat.AiModelChatRequest
import org.alterbit.aisme.chat.AiModelChatResponse
import org.alterbit.aisme.chat.AiModelClient
import org.alterbit.aisme.chat.toSingleUserPromptText
import org.alterbit.aisme.modelcatalog.ChatModelDescriptor
import org.slf4j.LoggerFactory

class EmbeddedLlamaAiModelClient(
    private val model: ChatModelDescriptor,
    private val chatApi: LlamaServerChatApi,
) : AiModelClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val modelId: String = model.id

    override fun chat(request: AiModelChatRequest): AiModelChatResponse {
        require(request.modelId == model.id) {
            "Llama runtime client for model '${model.id}' cannot handle model '${request.modelId}'"
        }

        val providerRequest = LlamaServerCompletionRequest(
            prompt = request.toSingleUserPromptText(),
            stream = false,
        )
        logger.info("Calling embedded llama runtime for model '{}'", model.id)
        val providerResponse = chatApi.complete(providerRequest)
        val answer = providerResponse.content.trim()

        check(answer.isNotBlank()) {
            "Llama runtime returned blank answer for model '${model.id}'"
        }
        logger.info("Embedded llama runtime returned non-blank answer for model '{}'", model.id)

        return AiModelChatResponse(
            modelId = request.modelId,
            answer = answer,
        )
    }
}

fun ChatModelDescriptor.requireEmbeddedAssetDirectory(): String =
    checkNotNull(assetDirectory) { "Embedded llama model '$id' requires assetDirectory" }

fun ChatModelDescriptor.requireEmbeddedServerExecutablePath(): String =
    checkNotNull(serverExecutablePath) { "Embedded llama model '$id' requires serverExecutablePath" }

fun ChatModelDescriptor.requireEmbeddedModelName(): String =
    checkNotNull(modelName) { "Embedded llama model '$id' requires modelName" }

fun ChatModelDescriptor.requireEmbeddedGgufFile(): String =
    checkNotNull(ggufFile) { "Embedded llama model '$id' requires ggufFile" }

fun ChatModelDescriptor.requireEmbeddedContextSize(): Int =
    checkNotNull(contextSize) { "Embedded llama model '$id' requires contextSize" }
