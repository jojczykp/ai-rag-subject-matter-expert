package org.alterbit.aisme.chat.ollama

import java.time.Duration
import org.alterbit.aisme.modelcatalog.ChatModelAvailability
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityChecker
import org.alterbit.aisme.modelcatalog.ChatModelDescriptor
import org.alterbit.aisme.modelcatalog.ChatModelRuntime
import org.springframework.stereotype.Component

@Component
class OllamaModelAvailabilityChecker(
    private val ollamaChatApiFactory: OllamaChatApiFactory,
) : ChatModelAvailabilityChecker {
    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.OLLAMA

    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability {
        val baseUrl = model.baseUrl ?: return ChatModelAvailability.MISCONFIGURED
        val modelName = model.modelName ?: return ChatModelAvailability.MISCONFIGURED

        return try {
            val availableModelNames = ollamaChatApiFactory
                .create(baseUrl = baseUrl, timeout = timeout)
                .modelNames()

            if (availableModelNames.any { it.matchesConfiguredOllamaModel(modelName) }) {
                ChatModelAvailability.AVAILABLE
            } else {
                ChatModelAvailability.UNAVAILABLE
            }
        } catch (ex: RuntimeException) {
            ChatModelAvailability.UNAVAILABLE
        }
    }

    private fun String.matchesConfiguredOllamaModel(configuredModelName: String): Boolean =
        this == configuredModelName || this == "$configuredModelName:latest"
}
