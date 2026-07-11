package org.alterbit.aisme.modelcatalog

class ChatModelUnavailableException(
    val modelId: String,
    val availability: ChatModelAvailability,
) : RuntimeException("Chat model is not available: $modelId ($availability)")
