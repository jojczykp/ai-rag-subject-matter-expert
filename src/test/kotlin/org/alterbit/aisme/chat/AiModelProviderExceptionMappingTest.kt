package org.alterbit.aisme.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
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
    fun `maps http timeout to provider timeout exception`() {
        val exception = ResourceAccessException("Request timed out", HttpTimeoutException("Request timed out"))
            .toAiModelProviderException(
                modelId = "cloud-gpt",
                provider = "OpenAI-compatible",
            )

        exception::class shouldBe AiModelProviderTimeoutException::class
        exception.modelId shouldBe "cloud-gpt"
        exception.provider shouldBe "OpenAI-compatible"
    }

    @Test
    fun `maps interrupted IO timeout to provider timeout exception`() {
        val exception = ResourceAccessException("Request interrupted", InterruptedIOException("Request interrupted"))
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

    @Test
    fun `includes http status when mapping provider response exception`() {
        val exception = HttpServerErrorException(HttpStatus.BAD_GATEWAY)
            .toAiModelProviderException(
                modelId = "cloud-gpt",
                provider = "OpenAI-compatible",
            )

        exception::class shouldBe AiModelProviderException::class
        exception.modelId shouldBe "cloud-gpt"
        exception.provider shouldBe "OpenAI-compatible"
        exception.message shouldContain "HTTP 502 BAD_GATEWAY"
    }
}
