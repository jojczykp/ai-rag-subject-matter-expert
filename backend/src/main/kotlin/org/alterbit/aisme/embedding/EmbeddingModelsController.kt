package org.alterbit.aisme.embedding

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class EmbeddingModelsController(
    private val embeddingModelRegistry: EmbeddingModelRegistry,
) {
    @GetMapping("/embedding-models")
    fun embeddingModels(): EmbeddingModelsResponseDto =
        EmbeddingModelsResponseDto(
            embeddingModels = embeddingModelRegistry.embeddingModels().map { it.toDto() },
        )
}
