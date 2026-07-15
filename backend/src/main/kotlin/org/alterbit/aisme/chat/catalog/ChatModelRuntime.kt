package org.alterbit.aisme.chat.catalog

import org.alterbit.aisme.chat.catalog.ChatModelRuntimeRequirement.REQUIRES_API_KEY
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeRequirement.REQUIRES_LLAMA_SERVER_EXECUTABLE
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeRequirement.REQUIRES_LOCAL_GGUF_MODEL
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeRequirement.REQUIRES_NETWORK
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeRequirement.REQUIRES_OLLAMA_SERVER

enum class ChatModelRuntime(
    val providerLabel: String,
    val runtimeRequirements: List<ChatModelRuntimeRequirement>,
) {
    SPRING_AI(
        providerLabel = "Spring AI",
        runtimeRequirements = listOf(REQUIRES_NETWORK),
    ),
    OPENAI_COMPATIBLE(
        providerLabel = "OpenAI-compatible",
        runtimeRequirements = listOf(REQUIRES_NETWORK, REQUIRES_API_KEY),
    ),
    OLLAMA(
        providerLabel = "Ollama",
        runtimeRequirements = listOf(REQUIRES_OLLAMA_SERVER),
    ),
    HUGGING_FACE_TGI(
        providerLabel = "Hugging Face TGI",
        runtimeRequirements = listOf(REQUIRES_NETWORK),
    ),
    EMBEDDED_LLAMA(
        providerLabel = "Embedded llama",
        runtimeRequirements = listOf(REQUIRES_LOCAL_GGUF_MODEL, REQUIRES_LLAMA_SERVER_EXECUTABLE),
    ),
}
