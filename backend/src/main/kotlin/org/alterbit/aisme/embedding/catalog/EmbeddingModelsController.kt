package org.alterbit.aisme.embedding.catalog

import java.time.Duration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class EmbeddingModelsController(
    private val embeddingModelRegistry: EmbeddingModelRegistry,
    private val embeddingModelAvailabilityService: EmbeddingModelAvailabilityService,
    private val embeddingProperties: EmbeddingProperties,
) {
    @GetMapping("/embedding-models")
    fun embeddingModels(): EmbeddingModelsResponseDto =
        EmbeddingModelsResponseDto(
            defaultEmbeddingModelId = embeddingModelRegistry.defaultModelId(),
            embeddingApiTimeoutSeconds = embeddingProperties.apiTimeout.toWholeSecondsRoundedUp(),
            embeddingModels = embeddingModelAvailabilityService
                .withAvailability(embeddingModelRegistry.embeddingModels())
                .map { it.toDto() },
        )
}

private fun Duration.toWholeSecondsRoundedUp(): Long =
    (toMillis() + 999) / 1000
