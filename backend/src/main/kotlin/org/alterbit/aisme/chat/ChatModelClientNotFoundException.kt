package org.alterbit.aisme.chat

class ChatModelClientNotFoundException(
    val modelId: String,
) : RuntimeException("AI model client not found: $modelId")
