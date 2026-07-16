package org.alterbit.aisme.chat

class FakeChatModelClient(
    override val modelId: String = "local-ollama-llama",
    private val responseFactory: (ChatModelRequest) -> ChatModelResponse = ::defaultResponse,
) : ChatModelClient {
    private val recordedRequests = mutableListOf<ChatModelRequest>()

    val requests: List<ChatModelRequest>
        get() = recordedRequests.toList()

    override fun chat(request: ChatModelRequest): ChatModelResponse {
        recordedRequests += request
        return responseFactory(request)
    }

    private companion object {
        fun defaultResponse(request: ChatModelRequest): ChatModelResponse =
            ChatModelResponse(
                answer = "Fake answer for: ${request.message}",
                modelId = request.modelId,
            )
    }
}
