package org.alterbit.aisme.chat.runtime.ollama

import java.time.Duration
import org.alterbit.aisme.chat.catalog.ChatModelAvailability
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityChecker
import org.alterbit.aisme.chat.catalog.ChatModelDescriptor
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OllamaModelAvailabilityChecker(
    private val ollamaChatApiFactory: OllamaChatApiFactory,
) : ChatModelAvailabilityChecker {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.OLLAMA

    override fun check(model: ChatModelDescriptor, apiTimeout: Duration): ChatModelAvailability {
        val baseUrl = model.baseUrl ?: return ChatModelAvailability.MISCONFIGURED.also {
            logger.warn("Ollama model '{}' is missing base URL", model.id)
        }
        val modelName = model.modelName ?: return ChatModelAvailability.MISCONFIGURED.also {
            logger.warn("Ollama model '{}' is missing model name", model.id)
        }

        return try {
            logger.info("Checking Ollama availability for model '{}'", model.id)
            val availableModelNames = ollamaChatApiFactory
                .create(baseUrl = baseUrl, apiTimeout = apiTimeout)
                .modelNames()

            if (availableModelNames.any { it.matchesConfiguredOllamaModel(modelName) }) {
                logger.info("Ollama model '{}' is available", model.id)
                ChatModelAvailability.AVAILABLE
            } else {
                logger.warn("Ollama model '{}' is not available from local Ollama", model.id)
                ChatModelAvailability.UNAVAILABLE
            }
        } catch (ex: RuntimeException) {
            logger.warn(
                "Ollama availability check failed for model '{}'",
                model.id,
                ex,
            )
            ChatModelAvailability.UNAVAILABLE
        }
    }

    private fun String.matchesConfiguredOllamaModel(configuredModelName: String): Boolean =
        this == configuredModelName || this == "$configuredModelName:latest"
}
