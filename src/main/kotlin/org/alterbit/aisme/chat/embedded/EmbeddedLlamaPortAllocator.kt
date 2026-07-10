package org.alterbit.aisme.chat.embedded

import java.net.ServerSocket
import org.springframework.stereotype.Component

@Component
class EphemeralEmbeddedLlamaPortAllocator(
    private val portSupplier: (() -> Int)? = null,
) {
    fun allocate(): Int =
        portSupplier?.invoke() ?: allocateEphemeralPort()

    private fun allocateEphemeralPort(): Int =
        ServerSocket(0).use { it.localPort }
}
