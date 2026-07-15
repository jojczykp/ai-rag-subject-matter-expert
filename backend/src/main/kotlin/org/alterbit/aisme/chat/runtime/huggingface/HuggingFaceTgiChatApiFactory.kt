package org.alterbit.aisme.chat.runtime.huggingface

import java.time.Duration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

interface HuggingFaceTgiChatApiFactory {
    fun create(
        baseUrl: String,
        apiKey: String?,
        apiTimeout: Duration,
    ): HuggingFaceTgiChatApi
}

@Component
class RestClientHuggingFaceTgiChatApiFactory : HuggingFaceTgiChatApiFactory {
    override fun create(
        baseUrl: String,
        apiKey: String?,
        apiTimeout: Duration,
    ): HuggingFaceTgiChatApi {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(apiTimeout)
            setReadTimeout(apiTimeout)
        }
        val restClientBuilder = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

        if (apiKey != null) {
            restClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
        }

        return RestClientHuggingFaceTgiChatApi(restClientBuilder.build())
    }
}

private class RestClientHuggingFaceTgiChatApi(
    private val restClient: RestClient,
) : HuggingFaceTgiChatApi {
    override fun generate(request: HuggingFaceTgiGenerateRequest): HuggingFaceTgiGenerateResponse =
        restClient.post()
            .uri("/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(HuggingFaceTgiGenerateResponse::class.java)
            ?: HuggingFaceTgiGenerateResponse()
}
