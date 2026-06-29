package org.alterbit.aisme.chat

class AiModelClientNotFoundException(
    val modelId: String,
) : RuntimeException("AI model client not found: $modelId")
