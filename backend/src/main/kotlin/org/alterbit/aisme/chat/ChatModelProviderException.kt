package org.alterbit.aisme.chat

open class ChatModelProviderException(
    val modelId: String,
    val provider: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class ChatModelProviderTimeoutException(
    modelId: String,
    provider: String,
    cause: Throwable,
) : ChatModelProviderException(
    modelId = modelId,
    provider = provider,
    message = "$provider provider timed out for model '$modelId'",
    cause = cause,
)
