package org.alterbit.aisme.chat.catalog

class ChatModelNotFoundException(
    val modelId: String,
) : RuntimeException("Configured chat model not found: $modelId")
