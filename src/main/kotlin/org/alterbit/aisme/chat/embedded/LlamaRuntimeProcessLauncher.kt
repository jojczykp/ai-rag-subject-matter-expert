package org.alterbit.aisme.chat.embedded

import org.springframework.stereotype.Component

fun interface LlamaRuntimeProcessLauncher {
    fun start(command: List<String>): Process
}

@Component
class ProcessBuilderLlamaRuntimeProcessLauncher : LlamaRuntimeProcessLauncher {
    override fun start(command: List<String>): Process =
        ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
}
