package org.alterbit.aisme.chat

import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import org.springframework.web.client.RestClientResponseException

fun RuntimeException.toAiModelProviderException(
    modelId: String,
    provider: String,
): AiModelProviderException =
    if (isTimeout()) {
        AiModelProviderTimeoutException(
            modelId = modelId,
            provider = provider,
            cause = this,
        )
    } else {
        AiModelProviderException(
            modelId = modelId,
            provider = provider,
            message = providerErrorMessage(
                modelId = modelId,
                provider = provider,
            ),
            cause = this,
        )
    }

private fun RuntimeException.providerErrorMessage(
    modelId: String,
    provider: String,
): String {
    val status = (this as? RestClientResponseException)?.statusCode
    return if (status == null) {
        "$provider provider failed for model '$modelId'"
    } else {
        "$provider provider failed for model '$modelId' with HTTP $status"
    }
}

private fun Throwable.isTimeout(): Boolean =
    causeChain().any {
        it is SocketTimeoutException ||
            it is HttpTimeoutException ||
            it is InterruptedIOException
    }

private fun Throwable.causeChain(): Sequence<Throwable> =
    generateSequence(this) { it.cause }
