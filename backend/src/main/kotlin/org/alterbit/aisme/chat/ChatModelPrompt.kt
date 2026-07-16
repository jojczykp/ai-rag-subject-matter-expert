package org.alterbit.aisme.chat

fun ChatModelRequest.toSingleUserPromptText(): String {
    if (contextChunks.isEmpty()) {
        return message
    }

    val context = contextChunks.joinToString(separator = "\n\n") { it.content }

    return buildString {
        appendLine("Context:")
        appendLine(context)
        appendLine()
        appendLine("Question:")
        append(message)
    }
}
