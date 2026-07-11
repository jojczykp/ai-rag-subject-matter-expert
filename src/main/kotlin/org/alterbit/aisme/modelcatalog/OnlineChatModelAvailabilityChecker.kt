package org.alterbit.aisme.modelcatalog

import java.time.Duration
import org.springframework.stereotype.Component

@Component
class OnlineChatModelAvailabilityChecker : ChatModelAvailabilityChecker {
    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.OPENAI_COMPATIBLE ||
            model.runtime == ChatModelRuntime.HUGGING_FACE_ENDPOINT ||
            model.runtime == ChatModelRuntime.SPRING_AI

    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
        when (model.runtime) {
            ChatModelRuntime.OPENAI_COMPATIBLE ->
                if (model.apiKey.isNullOrBlank()) {
                    ChatModelAvailability.MISCONFIGURED
                } else {
                    ChatModelAvailability.CONFIGURED
                }

            ChatModelRuntime.HUGGING_FACE_ENDPOINT,
            ChatModelRuntime.SPRING_AI,
            -> ChatModelAvailability.CONFIGURED

            ChatModelRuntime.OLLAMA,
            ChatModelRuntime.EMBEDDED_OFFLINE,
            -> ChatModelAvailability.MISCONFIGURED
        }
}
