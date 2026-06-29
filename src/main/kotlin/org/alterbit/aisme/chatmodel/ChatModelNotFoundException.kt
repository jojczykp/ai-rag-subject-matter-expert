package org.alterbit.aisme.chatmodel

class ChatModelNotFoundException(modelId: String) : RuntimeException("Configured chat model not found: $modelId")
