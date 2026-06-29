package org.alterbit.aisme.chatmodel

class ChatModelNotFoundException(
    val modelId: String,
) : RuntimeException("Configured chat model not found: $modelId")
