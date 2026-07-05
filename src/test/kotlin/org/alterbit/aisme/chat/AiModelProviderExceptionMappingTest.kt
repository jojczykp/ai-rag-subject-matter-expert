package org.alterbit.aisme.chat

import io.kotest.matchers.shouldBe
import java.net.SocketTimeoutException
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException

class AiModelProviderExceptionMappingTest {
    @Test
    fun `maps socket timeout to provider timeout exception`() {
        val exception = ResourceAccessException("Read timed out", SocketTimeoutException("Read timed out"))
            .toAiModelProviderException(
                modelId = "cloud-gpt",
                provider = "OpenAI-compatible",
            )

        exception::class shouldBe AiModelProviderTimeoutException::class
        exception.modelId shouldBe "cloud-gpt"
        exception.provider shouldBe "OpenAI-compatible"
    }

    @Test
    fun `maps other runtime exception to provider exception`() {
        val exception = IllegalStateException("provider returned invalid payload")
            .toAiModelProviderException(
                modelId = "cloud-gpt",
                provider = "OpenAI-compatible",
            )

        exception::class shouldBe AiModelProviderException::class
        exception.modelId shouldBe "cloud-gpt"
        exception.provider shouldBe "OpenAI-compatible"
    }

}
