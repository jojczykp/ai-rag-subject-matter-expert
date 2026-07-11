package org.alterbit.aisme.configuration

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chat.embedded.EmbeddedLlamaProperties
import org.alterbit.aisme.document.SubjectDocumentsProperties
import org.alterbit.aisme.embedding.EmbeddingModelProperties
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ConfiguredChatModelsProperties
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
            .withPropertyValues("aisme.embedding-model.dimensions=0")
            .run { context ->
                context.failureMessage() shouldContain "aisme.embedding-model.dimensions"
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
            .withPropertyValues("aisme.chat-model-availability.cache-ttl=0s")
            .run { context ->
                context.failureMessage() shouldContain "aisme.chat-model-availability.cache-ttl"
            }
    }

    @Test
    fun `fails binding invalid embedded llama configuration`() {
        propertyContext(EmbeddedLlamaPropertiesConfiguration::class.java)
            .withPropertyValues(
                "aisme.embedded-llama.asset-directory=./models/llama",
                "aisme.embedded-llama.server-executable-path=./models/llama/bin/llama-server",
                "aisme.embedded-llama.models[0].id=embedded-llama-example",
                "aisme.embedded-llama.models[0].enabled=true",
                "aisme.embedded-llama.models[0].display-name=Embedded Llama",
                "aisme.embedded-llama.models[0].gguf-file=models/llama.gguf",
                "aisme.embedded-llama.models[0].context-size=4096",
                "aisme.embedded-llama.models[0].sha256=invalid-sha",
            )
            .run { context ->
                context.failureMessage() shouldContain "aisme.embedded-llama.models.sha256"
            }
    }

    @Test
    fun `fails creating model catalog from invalid runtime and mode configuration`() {
        propertyContext(ChatModelCatalogConfiguration::class.java)
            .withPropertyValues(
                "aisme.chat-models[0].id=cloud-gpt",
                "aisme.chat-models[0].enabled=true",
                "aisme.chat-models[0].config.display-name=Cloud GPT",
                "aisme.chat-models[0].config.runtime=OPENAI_COMPATIBLE",
                "aisme.chat-models[0].config.mode=LOCAL_SERVER",
                "aisme.chat-models[0].config.available-offline=false",
                "aisme.chat-models[0].config.base-url=http://localhost:8000/v1",
                "aisme.chat-models[0].config.model-name=local-model",
            )
            .run { context ->
                context.failureMessage() shouldContain "aisme.chat-models[0].config.mode"
                context.failureMessage() shouldContain "ONLINE"
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
    @EnableConfigurationProperties(EmbeddingModelProperties::class)
    private class EmbeddingModelPropertiesConfiguration

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatProperties::class)
    private class ChatPropertiesConfiguration

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatModelAvailabilityProperties::class)
    private class ChatModelAvailabilityPropertiesConfiguration

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmbeddedLlamaProperties::class)
    private class EmbeddedLlamaPropertiesConfiguration

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConfiguredChatModelsProperties::class)
    private class ChatModelCatalogConfiguration {
        @Bean
        fun chatModelRegistry(properties: ConfiguredChatModelsProperties): ChatModelRegistry =
            ChatModelRegistry(properties)
    }
}
