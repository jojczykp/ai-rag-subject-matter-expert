package org.alterbit.aisme.embedding

import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

class OllamaEmbeddingClient(
    private val properties: EmbeddingModelProperties,
    private val embeddingApi: OllamaEmbeddingApi,
) : EmbeddingClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val modelId: String = properties.id
    override val model: EmbeddingModelMetadata = properties.metadata

    init {
        require(properties.runtime == EmbeddingModelRuntime.OLLAMA) {
            "Unsupported embedding model runtime: ${properties.runtime}"
        }
    }

    override fun embed(text: String): EmbeddingVector {
        require(text.isNotBlank()) { "text must not be blank" }

        logger.debug("Calling Ollama embedding provider for model '{}'", modelId)
        val response = embeddingApi.embed(
            OllamaEmbeddingRequest(
                model = properties.requireModelName(),
                input = text,
            ),
        )
        val values = response.embeddings.singleOrNull()
            ?: throw EmbeddingException("Ollama embedding response for model '$modelId' did not contain exactly one vector")

        require(values.size == model.dimensions) {
            "Ollama embedding dimensions ${values.size} did not match configured dimensions ${model.dimensions}"
        }

        return EmbeddingVector(
            values = values,
            model = model,
        )
    }
}

interface OllamaEmbeddingApi {
    fun embed(request: OllamaEmbeddingRequest): OllamaEmbeddingResponse

    fun modelNames(): Set<String>
}

data class OllamaEmbeddingRequest(
    val model: String,
    val input: String,
)

data class OllamaEmbeddingResponse(
    val embeddings: List<List<Double>> = emptyList(),
)

data class OllamaTagsResponse(
    val models: List<OllamaModelResponse> = emptyList(),
)

data class OllamaModelResponse(
    val name: String? = null,
    val model: String? = null,
)

interface OllamaEmbeddingApiFactory {
    fun create(baseUrl: String, apiTimeout: Duration = Duration.ofSeconds(60)): OllamaEmbeddingApi
}

@Component
class RestClientOllamaEmbeddingApiFactory : OllamaEmbeddingApiFactory {
    override fun create(baseUrl: String, apiTimeout: Duration): OllamaEmbeddingApi {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(apiTimeout)
            setReadTimeout(apiTimeout)
        }
        val restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build()

        return RestClientOllamaEmbeddingApi(restClient)
    }
}

private class RestClientOllamaEmbeddingApi(
    private val restClient: RestClient,
) : OllamaEmbeddingApi {
    override fun embed(request: OllamaEmbeddingRequest): OllamaEmbeddingResponse =
        restClient.post()
            .uri("/api/embed")
            .body(request)
            .retrieve()
            .body(OllamaEmbeddingResponse::class.java)
            ?: throw EmbeddingException("Ollama embedding response body was empty")

    override fun modelNames(): Set<String> =
        restClient.get()
            .uri("/api/tags")
            .retrieve()
            .body(OllamaTagsResponse::class.java)
            ?.models
            ?.flatMap { model -> listOfNotNull(model.name, model.model) }
            ?.toSet()
            ?: throw EmbeddingException("Ollama tags response body was empty")
}

@Component
class OllamaEmbeddingClientProvider(
    embeddingModelRegistry: EmbeddingModelRegistry,
    embeddingApiFactory: OllamaEmbeddingApiFactory,
    embeddingProperties: EmbeddingProperties,
) : EmbeddingClientProvider {
    private val clients: List<OllamaEmbeddingClient> = embeddingModelRegistry
        .enabledEmbeddingModelProperties()
        .filter { it.runtime == EmbeddingModelRuntime.OLLAMA }
        .map { properties ->
            OllamaEmbeddingClient(
                properties = properties,
                embeddingApi = embeddingApiFactory.create(
                    baseUrl = properties.requireBaseUrl(),
                    apiTimeout = embeddingProperties.apiTimeout,
                ),
            )
        }

    override fun clients(): List<EmbeddingClient> =
        clients
}

private fun EmbeddingModelProperties.requireBaseUrl(): String =
    checkNotNull(baseUrl) { "Ollama embedding model '$id' requires baseUrl" }

private fun EmbeddingModelProperties.requireModelName(): String =
    checkNotNull(modelName) { "Ollama embedding model '$id' requires modelName" }
