package org.alterbit.aisme.chat

open class AiModelProviderException(
    val modelId: String,
    val provider: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class AiModelProviderTimeoutException(
    modelId: String,
    provider: String,
    cause: Throwable,
) : AiModelProviderException(
    modelId = modelId,
    provider = provider,
    message = "$provider provider timed out for model '$modelId'",
    cause = cause,
)
