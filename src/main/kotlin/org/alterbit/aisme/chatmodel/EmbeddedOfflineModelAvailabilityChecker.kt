package org.alterbit.aisme.chatmodel

import java.time.Duration
import org.springframework.stereotype.Component

@Component
class EmbeddedOfflineModelAvailabilityChecker : ChatModelAvailabilityChecker {
    override fun supports(model: ChatModelDescriptor): Boolean =
        model.runtime == ChatModelRuntime.EMBEDDED_OFFLINE

    override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
        if (model.mode == ChatModelMode.EMBEDDED_OFFLINE && model.availableOffline) {
            ChatModelAvailability.AVAILABLE
        } else {
            ChatModelAvailability.MISCONFIGURED
        }
}
