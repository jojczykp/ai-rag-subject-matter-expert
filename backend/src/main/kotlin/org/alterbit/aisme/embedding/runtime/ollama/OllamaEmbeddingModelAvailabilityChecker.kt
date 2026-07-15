package org.alterbit.aisme.embedding.runtime.ollama

import java.time.Duration
import org.alterbit.aisme.embedding.catalog.EmbeddingModelAvailability
import org.alterbit.aisme.embedding.catalog.EmbeddingModelAvailabilityChecker
import org.alterbit.aisme.embedding.catalog.EmbeddingModelDescriptor
import org.alterbit.aisme.embedding.catalog.EmbeddingModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OllamaEmbeddingModelAvailabilityChecker(
    private val ollamaEmbeddingApiFactory: OllamaEmbeddingApiFactory,
) : EmbeddingModelAvailabilityChecker {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(model: EmbeddingModelDescriptor): Boolean =
        model.runtime == EmbeddingModelRuntime.OLLAMA

    override fun check(model: EmbeddingModelDescriptor, apiTimeout: Duration): EmbeddingModelAvailability {
        val baseUrl = model.baseUrl ?: return EmbeddingModelAvailability.MISCONFIGURED.also {
            logger.warn("Ollama embedding model '{}' is missing base URL", model.id)
        }
        val modelName = model.modelName ?: return EmbeddingModelAvailability.MISCONFIGURED.also {
            logger.warn("Ollama embedding model '{}' is missing model name", model.id)
        }

        return try {
            logger.info("Checking Ollama embedding availability for model '{}'", model.id)
            val availableModelNames = ollamaEmbeddingApiFactory
                .create(baseUrl = baseUrl, apiTimeout = apiTimeout)
                .modelNames()

            if (availableModelNames.any { it.matchesConfiguredOllamaModel(modelName) }) {
                logger.info("Ollama embedding model '{}' is available", model.id)
                EmbeddingModelAvailability.AVAILABLE
            } else {
                logger.warn("Ollama embedding model '{}' is not available from local Ollama", model.id)
                EmbeddingModelAvailability.UNAVAILABLE
            }
        } catch (ex: RuntimeException) {
            logger.warn(
                "Ollama embedding availability check failed for model '{}'",
                model.id,
                ex,
            )
            EmbeddingModelAvailability.UNAVAILABLE
        }
    }

    private fun String.matchesConfiguredOllamaModel(configuredModelName: String): Boolean =
        this == configuredModelName || this == "$configuredModelName:latest"
}
