package org.alterbit.aisme.chat.catalog

import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ChatModelAvailabilityService(
    private val properties: ChatModelAvailabilityProperties,
    private val checkers: List<ChatModelAvailabilityChecker> = emptyList(),
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val cachedAvailabilityByModelId = ConcurrentHashMap<String, CachedAvailability>()

    fun withAvailability(model: ChatModelDescriptor): ChatModelDescriptor {
        val checker = checkers.firstOrNull { it.supports(model) }
            ?: return model.copy(availability = ChatModelAvailability.CONFIGURED).also {
                logger.info(
                    "No availability checker for chat model '{}'; using '{}'",
                    model.id,
                    ChatModelAvailability.CONFIGURED,
                )
            }

        val availability = cachedAvailability(model)
            ?: checker.check(
                model = model,
                apiTimeout = properties.timeout,
            ).also { cacheAvailability(model, it) }
                .also {
                    logger.info(
                        "Checked availability for chat model '{}' with result '{}'",
                        model.id,
                        it,
                    )
                }

        return model.copy(availability = availability)
    }

    fun withAvailability(models: List<ChatModelDescriptor>): List<ChatModelDescriptor> =
        models.map(::withAvailability)

    private fun cachedAvailability(model: ChatModelDescriptor): ChatModelAvailability? =
        cachedAvailabilityByModelId[model.id]
            ?.takeIf { it.expiresAt.isAfter(clock.instant()) }
            ?.availability
            ?.also {
                logger.info("Using cached availability '{}' for chat model '{}'", it, model.id)
            }

    private fun cacheAvailability(model: ChatModelDescriptor, availability: ChatModelAvailability) {
        cachedAvailabilityByModelId[model.id] = CachedAvailability(
            availability = availability,
            expiresAt = clock.instant().plus(properties.cacheTtl),
        )
    }

    private data class CachedAvailability(
        val availability: ChatModelAvailability,
        val expiresAt: Instant,
    )
}
