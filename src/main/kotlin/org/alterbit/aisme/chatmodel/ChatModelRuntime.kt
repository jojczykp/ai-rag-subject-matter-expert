package org.alterbit.aisme.chatmodel

enum class ChatModelRuntime(
    val providerLabel: String,
) {
    SPRING_AI("Spring AI"),
    OPENAI_COMPATIBLE("OpenAI-compatible"),
    OLLAMA("Ollama"),
    HUGGING_FACE_ENDPOINT("Hugging Face TGI"),
    EMBEDDED_OFFLINE("Embedded offline"),
}
