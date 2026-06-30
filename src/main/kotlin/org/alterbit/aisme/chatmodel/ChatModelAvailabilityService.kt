package org.alterbit.aisme.chatmodel

import org.springframework.stereotype.Service

@Service
class ChatModelAvailabilityService(
    private val properties: ChatModelAvailabilityProperties,
    private val checkers: List<ChatModelAvailabilityChecker> = emptyList(),
) {
    fun withAvailability(model: ChatModelDescriptor): ChatModelDescriptor {
        val checker = checkers.firstOrNull { it.supports(model) }
            ?: return model.copy(availability = ChatModelAvailability.CONFIGURED)

        return model.copy(
            availability = checker.check(
                model = model,
                timeout = properties.timeout,
            ),
        )
    }

    fun withAvailability(models: List<ChatModelDescriptor>): List<ChatModelDescriptor> =
        models.map(::withAvailability)
}
