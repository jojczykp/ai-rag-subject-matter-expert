package org.alterbit.aisme.chatmodel

import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Service

@Service
class ChatModelAvailabilityService(
    private val properties: ChatModelAvailabilityProperties,
    private val checkers: List<ChatModelAvailabilityChecker> = emptyList(),
    private val clock: Clock,
) {
    private val cachedAvailabilityByModelId = ConcurrentHashMap<String, CachedAvailability>()

    fun withAvailability(model: ChatModelDescriptor): ChatModelDescriptor {
        val checker = checkers.firstOrNull { it.supports(model) }
            ?: return model.copy(availability = ChatModelAvailability.CONFIGURED)

        val availability = cachedAvailability(model)
            ?: checker.check(
                model = model,
                timeout = properties.timeout,
            ).also { cacheAvailability(model, it) }

        return model.copy(availability = availability)
    }

    fun withAvailability(models: List<ChatModelDescriptor>): List<ChatModelDescriptor> =
        models.map(::withAvailability)

    private fun cachedAvailability(model: ChatModelDescriptor): ChatModelAvailability? =
        cachedAvailabilityByModelId[model.id]
            ?.takeIf { it.expiresAt.isAfter(clock.instant()) }
            ?.availability

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
