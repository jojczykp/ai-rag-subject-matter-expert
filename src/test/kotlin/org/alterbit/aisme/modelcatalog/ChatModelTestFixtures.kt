package org.alterbit.aisme.modelcatalog

fun chatModel(
    id: String = "local-ollama-llama",
    displayName: String = "Local Ollama Llama",
    runtime: ChatModelRuntime = ChatModelRuntime.OLLAMA,
    mode: ChatModelMode = ChatModelMode.LOCAL_SERVER,
    availableOffline: Boolean = false,
    availability: ChatModelAvailability = ChatModelAvailability.CONFIGURED,
    baseUrl: String? = "http://localhost:11434",
    modelName: String? = "llama3.2",
    apiKey: String? = null,
): ChatModelDescriptor =
    ChatModelDescriptor(
        id = id,
        displayName = displayName,
        runtime = runtime,
        mode = mode,
        availableOffline = availableOffline,
        availability = availability,
        baseUrl = baseUrl,
        modelName = modelName,
        apiKey = apiKey,
    )
