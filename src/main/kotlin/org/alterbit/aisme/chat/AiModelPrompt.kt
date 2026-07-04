package org.alterbit.aisme.chat

fun AiModelChatRequest.toSingleUserPromptText(): String {
    if (contextChunks.isEmpty()) {
        return message
    }

    val context = contextChunks.joinToString(separator = "\n\n") { chunk ->
        "[${chunk.resourcePath}#${chunk.chunkIndex}]\n${chunk.content}"
    }

    return buildString {
        appendLine("Context:")
        appendLine(context)
        appendLine()
        appendLine("Question:")
        append(message)
    }
}
