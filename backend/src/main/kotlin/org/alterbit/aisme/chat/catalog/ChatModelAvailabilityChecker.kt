package org.alterbit.aisme.chat.catalog

import java.time.Duration

interface ChatModelAvailabilityChecker {
    fun supports(model: ChatModelDescriptor): Boolean

    fun check(model: ChatModelDescriptor, apiTimeout: Duration): ChatModelAvailability
}
