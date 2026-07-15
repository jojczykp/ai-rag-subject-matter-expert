package org.alterbit.aisme.chat.runtime.openai

import java.time.Duration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

interface OpenAiCompatibleChatApiFactory {
    fun create(
        baseUrl: String,
        apiKey: String,
        apiTimeout: Duration,
    ): OpenAiCompatibleChatApi
}

@Component
class RestClientOpenAiCompatibleChatApiFactory : OpenAiCompatibleChatApiFactory {
    override fun create(
        baseUrl: String,
        apiKey: String,
        apiTimeout: Duration,
    ): OpenAiCompatibleChatApi {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(apiTimeout)
            setReadTimeout(apiTimeout)
        }
        val restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

        return RestClientOpenAiCompatibleChatApi(restClient)
    }
}

private class RestClientOpenAiCompatibleChatApi(
    private val restClient: RestClient,
) : OpenAiCompatibleChatApi {
    override fun chat(request: OpenAiCompatibleChatRequest): OpenAiCompatibleChatResponse =
        restClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(OpenAiCompatibleChatResponse::class.java)
            ?: OpenAiCompatibleChatResponse()
}
