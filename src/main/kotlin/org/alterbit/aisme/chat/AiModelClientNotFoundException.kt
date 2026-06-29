package org.alterbit.aisme.chat

class AiModelClientNotFoundException(modelId: String) : RuntimeException("AI model client not found: $modelId")
