package org.alterbit.aisme.chat

class FakeAiModelClient(
    private val responseFactory: (AiModelChatRequest) -> AiModelChatResponse = ::defaultResponse,
) : AiModelClient {
    private val recordedRequests = mutableListOf<AiModelChatRequest>()

    val requests: List<AiModelChatRequest>
        get() = recordedRequests.toList()

    override fun chat(request: AiModelChatRequest): AiModelChatResponse {
        recordedRequests += request
        return responseFactory(request)
    }

    private companion object {
        fun defaultResponse(request: AiModelChatRequest): AiModelChatResponse =
            AiModelChatResponse(
                answer = "Fake answer for: ${request.message}",
                modelId = request.modelId,
            )
    }
}
