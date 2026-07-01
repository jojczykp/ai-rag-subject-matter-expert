package org.alterbit.aisme.chat.ollama

import java.time.Duration
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

interface OllamaChatApiFactory {
    fun create(baseUrl: String, timeout: Duration): OllamaChatApi
}

@Component
class SpringAiOllamaChatApiFactory : OllamaChatApiFactory {
    override fun create(baseUrl: String, timeout: Duration): OllamaChatApi {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(timeout)
            setReadTimeout(timeout)
        }
        val restClientBuilder = RestClient.builder()
            .requestFactory(requestFactory)

        return OllamaChatApi(
            OllamaApi.builder()
                .baseUrl(baseUrl)
                .restClientBuilder(restClientBuilder)
                .build()::chat,
        )
    }
}
