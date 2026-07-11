package org.alterbit.aisme.modelcatalog

enum class ChatModelRuntimeRequirement {
    REQUIRES_NETWORK,
    REQUIRES_API_KEY,
    REQUIRES_OLLAMA_SERVER,
    REQUIRES_LOCAL_GGUF_MODEL,
    REQUIRES_LLAMA_SERVER_EXECUTABLE,
}
