package org.alterbit.aisme.chat.ollama

import java.time.Duration
import org.alterbit.aisme.modelcatalog.ChatModelAvailability
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityChecker
import org.alterbit.aisme.modelcatalog.ChatModelDescriptor
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OllamaModelAvailabilityChecker(
    private val ollamaChatApiFactory: OllamaChatApiFactory,
) : ChatModelAvailabilityChecker {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.OLLAMA

    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability {
        val baseUrl = model.baseUrl ?: return ChatModelAvailability.MISCONFIGURED.also {
            logger.warn("Ollama model '{}' is missing base URL", model.id)
        }
        val modelName = model.modelName ?: return ChatModelAvailability.MISCONFIGURED.also {
            logger.warn("Ollama model '{}' is missing model name", model.id)
        }

        return try {
            logger.info("Checking Ollama availability for model '{}'", model.id)
            val availableModelNames = ollamaChatApiFactory
                .create(baseUrl = baseUrl, timeout = timeout)
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
                "Ollama availability check failed for model '{}' with exception '{}'",
                model.id,
                ex.javaClass.simpleName,
            )
            ChatModelAvailability.UNAVAILABLE
        }
    }

    private fun String.matchesConfiguredOllamaModel(configuredModelName: String): Boolean =
        this == configuredModelName || this == "$configuredModelName:latest"
}
