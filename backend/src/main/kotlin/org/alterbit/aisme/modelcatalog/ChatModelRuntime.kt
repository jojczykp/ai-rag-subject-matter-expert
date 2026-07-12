package org.alterbit.aisme.modelcatalog

import org.alterbit.aisme.modelcatalog.ChatModelRuntimeRequirement.REQUIRES_API_KEY
import org.alterbit.aisme.modelcatalog.ChatModelRuntimeRequirement.REQUIRES_LLAMA_SERVER_EXECUTABLE
import org.alterbit.aisme.modelcatalog.ChatModelRuntimeRequirement.REQUIRES_LOCAL_GGUF_MODEL
import org.alterbit.aisme.modelcatalog.ChatModelRuntimeRequirement.REQUIRES_NETWORK
import org.alterbit.aisme.modelcatalog.ChatModelRuntimeRequirement.REQUIRES_OLLAMA_SERVER

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
    HUGGING_FACE_ENDPOINT(
        providerLabel = "Hugging Face TGI",
        runtimeRequirements = listOf(REQUIRES_NETWORK),
    ),
    EMBEDDED_OFFLINE(
        providerLabel = "Embedded offline",
        runtimeRequirements = listOf(REQUIRES_LOCAL_GGUF_MODEL, REQUIRES_LLAMA_SERVER_EXECUTABLE),
    ),
}
