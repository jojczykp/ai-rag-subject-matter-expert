package org.alterbit.aisme.chatmodel

class ChatModelUnavailableException(
    val modelId: String,
    val availability: ChatModelAvailability,
) : RuntimeException("Chat model is not available: $modelId ($availability)")
