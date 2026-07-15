package org.alterbit.aisme.embedding.catalog

import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EmbeddingModelAvailabilityService(
    private val properties: EmbeddingModelAvailabilityProperties,
    private val checkers: List<EmbeddingModelAvailabilityChecker> = emptyList(),
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val cachedAvailabilityByModelId = ConcurrentHashMap<String, CachedAvailability>()

    fun withAvailability(model: EmbeddingModelDescriptor): EmbeddingModelDescriptor {
        if (!model.enabled) {
            return model.copy(availability = EmbeddingModelAvailability.UNAVAILABLE)
        }

        val checker = checkers.firstOrNull { it.supports(model) }
            ?: return model.copy(availability = EmbeddingModelAvailability.CONFIGURED).also {
                logger.info(
                    "No availability checker for embedding model '{}'; using '{}'",
                    model.id,
                    EmbeddingModelAvailability.CONFIGURED,
                )
            }

        val availability = cachedAvailability(model)
            ?: checker.check(
                model = model,
                apiTimeout = properties.timeout,
            ).also { cacheAvailability(model, it) }
                .also {
                    logger.info(
                        "Checked availability for embedding model '{}' with result '{}'",
                        model.id,
                        it,
                    )
                }

        return model.copy(availability = availability)
    }

    fun withAvailability(models: List<EmbeddingModelDescriptor>): List<EmbeddingModelDescriptor> =
        models.map(::withAvailability)

    private fun cachedAvailability(model: EmbeddingModelDescriptor): EmbeddingModelAvailability? =
        cachedAvailabilityByModelId[model.id]
            ?.takeIf { it.expiresAt.isAfter(clock.instant()) }
            ?.availability
            ?.also {
                logger.info("Using cached availability '{}' for embedding model '{}'", it, model.id)
            }

    private fun cacheAvailability(model: EmbeddingModelDescriptor, availability: EmbeddingModelAvailability) {
        cachedAvailabilityByModelId[model.id] = CachedAvailability(
            availability = availability,
            expiresAt = clock.instant().plus(properties.cacheTtl),
        )
    }

    private data class CachedAvailability(
        val availability: EmbeddingModelAvailability,
        val expiresAt: Instant,
    )
}
