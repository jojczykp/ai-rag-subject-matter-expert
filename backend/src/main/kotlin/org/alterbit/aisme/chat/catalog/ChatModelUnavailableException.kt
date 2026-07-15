package org.alterbit.aisme.chat.catalog

class ChatModelUnavailableException(
    val modelId: String,
    val availability: ChatModelAvailability,
) : RuntimeException("Chat model is not available: $modelId ($availability)")
