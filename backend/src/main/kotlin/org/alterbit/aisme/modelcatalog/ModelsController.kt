package org.alterbit.aisme.modelcatalog

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ModelsController(
    private val chatModelRegistry: ChatModelRegistry,
    private val chatModelAvailabilityService: ChatModelAvailabilityService,
) {
    @GetMapping("/models")
    fun models(): ModelsResponseDto =
        ModelsResponseDto(
            models = chatModelAvailabilityService
                .withAvailability(chatModelRegistry.chatModels())
                .map { it.toDto() },
        )
}
