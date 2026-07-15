package org.alterbit.aisme.chat.catalog

import java.time.Duration
import org.alterbit.aisme.chat.ChatProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatModelsController(
    private val chatModelRegistry: ChatModelRegistry,
    private val chatModelAvailabilityService: ChatModelAvailabilityService,
    private val chatProperties: ChatProperties,
) {
    @GetMapping("/chat-models")
    fun chatModels(): ChatModelsResponseDto =
        ChatModelsResponseDto(
            defaultChatModelId = chatModelRegistry.defaultModelId(),
            chatApiTimeoutSeconds = chatProperties.apiTimeout.toWholeSecondsRoundedUp(),
            chatModels = chatModelAvailabilityService
                .withAvailability(chatModelRegistry.chatModels())
                .map { it.toDto() },
        )
}

private fun Duration.toWholeSecondsRoundedUp(): Long =
    (toMillis() + 999) / 1000
