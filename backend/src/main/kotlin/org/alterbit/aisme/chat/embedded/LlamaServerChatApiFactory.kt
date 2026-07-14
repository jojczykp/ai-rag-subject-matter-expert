package org.alterbit.aisme.chat.embedded

import java.time.Duration
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

interface LlamaServerChatApiFactory {
    fun create(baseUrl: String, apiTimeout: Duration): LlamaServerChatApi
}

@Component
class RestClientLlamaServerChatApiFactory : LlamaServerChatApiFactory {
    override fun create(baseUrl: String, apiTimeout: Duration): LlamaServerChatApi {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(apiTimeout)
            setReadTimeout(apiTimeout)
        }
        val restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build()

        return RestClientLlamaServerChatApi(restClient)
    }
}

private class RestClientLlamaServerChatApi(
    private val restClient: RestClient,
) : LlamaServerChatApi {
    override fun complete(request: LlamaServerCompletionRequest): LlamaServerCompletionResponse =
        restClient.post()
            .uri("/completion")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(LlamaServerCompletionResponse::class.java)
            ?: LlamaServerCompletionResponse(content = "")
}
