package org.alterbit.aisme.chatmodel

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ModelsController(
    private val chatModelRegistry: ChatModelRegistry,
) {
    @GetMapping("/models")
    fun models(): ModelsResponseDto =
        ModelsResponseDto(
            models = chatModelRegistry.chatModels().map { it.toDto() },
        )
}
