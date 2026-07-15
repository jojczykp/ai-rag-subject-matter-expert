package org.alterbit.aisme.modelcatalog

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatModelsController(
    private val chatModelRegistry: ChatModelRegistry,
    private val chatModelAvailabilityService: ChatModelAvailabilityService,
) {
    @GetMapping("/chat-models")
    fun chatModels(): ChatModelsResponseDto =
        ChatModelsResponseDto(
            chatModels = chatModelAvailabilityService
                .withAvailability(chatModelRegistry.chatModels())
                .map { it.toDto() },
        )
}
