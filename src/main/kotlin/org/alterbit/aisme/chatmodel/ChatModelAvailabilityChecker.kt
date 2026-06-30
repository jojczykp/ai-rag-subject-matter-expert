package org.alterbit.aisme.chatmodel

import java.time.Duration

interface ChatModelAvailabilityChecker {
    fun supports(model: ChatModelDescriptor): Boolean

    fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability
}
