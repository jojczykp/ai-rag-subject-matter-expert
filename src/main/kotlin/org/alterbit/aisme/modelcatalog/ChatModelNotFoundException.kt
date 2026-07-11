package org.alterbit.aisme.modelcatalog

class ChatModelNotFoundException(
    val modelId: String,
) : RuntimeException("Configured chat model not found: $modelId")
