package org.alterbit.aisme.modelcatalog

import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OnlineChatModelAvailabilityChecker : ChatModelAvailabilityChecker {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.OPENAI_COMPATIBLE ||
            model.runtime == ChatModelRuntime.HUGGING_FACE_TGI ||
            model.runtime == ChatModelRuntime.SPRING_AI

    override fun check(model: ChatModelDescriptor, apiTimeout: Duration): ChatModelAvailability =
        when (model.runtime) {
            ChatModelRuntime.OPENAI_COMPATIBLE ->
                if (model.apiKey.isNullOrBlank()) {
                    logger.warn("OpenAI-compatible model '{}' is missing an API key", model.id)
                    ChatModelAvailability.MISCONFIGURED
                } else {
                    logger.info("OpenAI-compatible model '{}' has required API key configuration", model.id)
                    ChatModelAvailability.CONFIGURED
                }

            ChatModelRuntime.HUGGING_FACE_TGI,
            ChatModelRuntime.SPRING_AI,
            -> {
                logger.info("Online model '{}' is configured for runtime '{}'", model.id, model.runtime)
                ChatModelAvailability.CONFIGURED
            }

            ChatModelRuntime.OLLAMA,
            ChatModelRuntime.EMBEDDED_LLAMA, -> {
                logger.warn("Online availability checker does not support model '{}' with runtime '{}'", model.id, model.runtime)
                ChatModelAvailability.MISCONFIGURED
            }
        }
}
