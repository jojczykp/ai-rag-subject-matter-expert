package org.alterbit.aisme.chat.embedded

import org.springframework.stereotype.Component

fun interface EmbeddedLlamaProcessLauncher {
    fun start(command: List<String>): Process
}

@Component
class ProcessBuilderEmbeddedLlamaProcessLauncher : EmbeddedLlamaProcessLauncher {
    override fun start(command: List<String>): Process =
        ProcessBuilder(command)
            .start()
}
