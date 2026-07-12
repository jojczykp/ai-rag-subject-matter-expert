package org.alterbit.aisme.modelcatalog

import java.time.Duration

interface ChatModelAvailabilityChecker {
    fun supports(model: ChatModelDescriptor): Boolean

    fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability
}
