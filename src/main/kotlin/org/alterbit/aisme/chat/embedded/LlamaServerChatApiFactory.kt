package org.alterbit.aisme.chat.embedded

import java.time.Duration
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

interface LlamaServerChatApiFactory {
    fun create(baseUrl: String, timeout: Duration): LlamaServerChatApi
}

@Component
class RestClientLlamaServerChatApiFactory : LlamaServerChatApiFactory {
    override fun create(baseUrl: String, timeout: Duration): LlamaServerChatApi {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(timeout)
            setReadTimeout(timeout)
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
    override fun chat(request: LlamaServerChatRequest): LlamaServerChatResponse =
        restClient.post()
            .uri("/v1/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(LlamaServerChatResponse::class.java)
            ?: LlamaServerChatResponse()
}
