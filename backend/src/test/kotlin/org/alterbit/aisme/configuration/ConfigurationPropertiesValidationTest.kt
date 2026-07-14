package org.alterbit.aisme.configuration

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.document.SubjectDocumentsProperties
import org.alterbit.aisme.embedding.EmbeddingProperties
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelsProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class ConfigurationPropertiesValidationTest {
    @Test
    fun `fails binding invalid document chunking configuration`() {
        propertyContext(DocumentsPropertiesConfiguration::class.java)
            .withPropertyValues(
                "aisme.documents.chunk-size=100",
                "aisme.documents.chunk-overlap=100",
            )
            .run { context ->
                context.failureMessage() shouldContain "aisme.documents.chunk-overlap"
                context.failureMessage() shouldContain "smaller than chunk-size"
            }
    }

    @Test
    fun `fails binding invalid embedding model configuration`() {
        propertyContext(EmbeddingModelPropertiesConfiguration::class.java)
            .withPropertyValues("aisme.embedding.models.local-bge-small.dimensions=0")
            .run { context ->
                context.failureMessage() shouldContain "aisme.embedding.models.dimensions"
            }
    }

    @Test
    fun `fails binding invalid chat configuration`() {
        propertyContext(ChatPropertiesConfiguration::class.java)
            .withPropertyValues("aisme.chat.relevant-chunk-limit=0")
            .run { context ->
                context.failureMessage() shouldContain "aisme.chat.relevant-chunk-limit"
            }
    }

    @Test
    fun `fails binding invalid chat model availability configuration`() {
        propertyContext(ChatModelAvailabilityPropertiesConfiguration::class.java)
            .withPropertyValues("aisme.chat.model-availability.cache-ttl=0s")
            .run { context ->
                context.failureMessage() shouldContain "aisme.chat.model-availability.cache-ttl"
            }
    }

    @Test
    fun `fails creating model catalog when model references unknown runtime`() {
        propertyContext(ChatModelCatalogConfiguration::class.java)
            .withPropertyValues(
                "aisme.chat.runtimes.local-ollama.type=OLLAMA",
                "aisme.chat.runtimes.local-ollama.base-url=http://localhost:11434",
                "aisme.chat.models.cloud-gpt.enabled=true",
                "aisme.chat.models.cloud-gpt.display-name=Cloud GPT",
                "aisme.chat.models.cloud-gpt.runtime.id=missing-runtime",
                "aisme.chat.models.cloud-gpt.runtime.model-name=local-model",
            )
            .run { context ->
                context.failureMessage() shouldContain "runtime.id"
                context.failureMessage() shouldContain "missing-runtime"
            }
    }

    private fun propertyContext(configuration: Class<*>): ApplicationContextRunner =
        ApplicationContextRunner().withUserConfiguration(configuration)

    private fun org.springframework.boot.test.context.assertj.AssertableApplicationContext.failureMessage(): String =
        startupFailure
            .shouldNotBeNull()
            .stackTraceToString()

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SubjectDocumentsProperties::class)
    private class DocumentsPropertiesConfiguration

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddingProperties::class)
    private class EmbeddingModelPropertiesConfiguration

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatProperties::class)
    private class ChatPropertiesConfiguration

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatModelAvailabilityProperties::class)
    private class ChatModelAvailabilityPropertiesConfiguration

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatModelsProperties::class)
    private class ChatModelCatalogConfiguration {
        @Bean
        fun chatModelRegistry(properties: ChatModelsProperties): ChatModelRegistry =
            ChatModelRegistry(properties)
    }
}
