package org.alterbit.aisme.chat.api

import java.time.Clock
import java.time.Duration
import org.alterbit.aisme.chat.catalog.ChatProperties
import org.alterbit.aisme.chat.catalog.ChatModelAvailability
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityChecker
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.chat.catalog.ChatModelAvailabilityService
import org.alterbit.aisme.chat.catalog.ChatModelDescriptor
import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelsProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    classes = [ChatModelsControllerTestContext::class],
    properties = [
        "aisme.chat.default-model-id=local-ollama-llama",
        "aisme.chat.models.local-ollama-llama.enabled=true",
        "aisme.chat.models.local-ollama-llama.display-order=10",
        "aisme.chat.models.local-ollama-llama.display-name=Local Ollama Llama",
        "aisme.chat.models.local-ollama-llama.description=Local Ollama model for chat requests.",
        "aisme.chat.models.local-ollama-llama.runtime.id=local-ollama",
        "aisme.chat.models.local-ollama-llama.runtime.model-name=llama3.2",
        "aisme.chat.models.cloud-gpt.enabled=true",
        "aisme.chat.models.cloud-gpt.display-order=20",
        "aisme.chat.models.cloud-gpt.display-name=Cloud GPT",
        "aisme.chat.models.cloud-gpt.description=Cloud model for online chat requests.",
        "aisme.chat.models.cloud-gpt.runtime.id=spring-ai",
        "aisme.chat.models.embedded-qwen-0-5b.enabled=false",
        "aisme.chat.models.embedded-qwen-1-5b.enabled=false",
        "aisme.chat.models.embedded-qwen-3b.enabled=false",
        "aisme.chat.models.embedded-mistral-7b.enabled=false",
        "aisme.chat.models.openai-compatible-example.enabled=false",
        "aisme.chat.models.hugging-face-tgi-example.enabled=false",
    ],
)
@AutoConfigureMockMvc
class ChatModelsControllerTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `lists chat models`() {
        mockMvc.get("/chat-models")
            .andExpect {
                status { isOk() }
                jsonPath("$.defaultChatModelId") {
                    value("local-ollama-llama")
                }
                jsonPath("$.chatApiTimeoutSeconds") {
                    value(60)
                }
                jsonPath("$.chatModels.length()") {
                    value(2)
                }
                jsonPath("$.chatModels[0].id") {
                    value("local-ollama-llama")
                }
                jsonPath("$.chatModels[0].displayName") {
                    value("Local Ollama Llama")
                }
                jsonPath("$.chatModels[0].description") {
                    value("Local Ollama model for chat requests.")
                }
                jsonPath("$.chatModels[0].runtime") {
                    value("OLLAMA")
                }
                jsonPath("$.chatModels[0].mode") {
                    value("LOCAL_SERVER")
                }
                jsonPath("$.chatModels[0].availability") {
                    value("AVAILABLE")
                }
                jsonPath("$.chatModels[0].availableOffline") {
                    value(false)
                }
                jsonPath("$.chatModels[0].promptsMayLeaveLocalMachine") {
                    value(false)
                }
                jsonPath("$.chatModels[0].capabilities[0]") {
                    value("CHAT")
                }
                jsonPath("$.chatModels[0].runtimeRequirements[0]") {
                    value("REQUIRES_OLLAMA_SERVER")
                }
                jsonPath("$.chatModels[0].baseUrl") {
                    doesNotExist()
                }
                jsonPath("$.chatModels[1].id") {
                    value("cloud-gpt")
                }
                jsonPath("$.chatModels[1].description") {
                    value("Cloud model for online chat requests.")
                }
                jsonPath("$.chatModels[1].availability") {
                    value("CONFIGURED")
                }
                jsonPath("$.chatModels[1].promptsMayLeaveLocalMachine") {
                    value(true)
                }
                jsonPath("$.chatModels[1].capabilities[0]") {
                    value("CHAT")
                }
                jsonPath("$.chatModels[1].runtimeRequirements[0]") {
                    value("REQUIRES_NETWORK")
                }
            }
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration(
    exclude = [
        DataSourceAutoConfiguration::class,
        FlywayAutoConfiguration::class,
    ],
)
@EnableConfigurationProperties(
    ChatModelAvailabilityProperties::class,
    ChatModelsProperties::class,
    ChatProperties::class,
)
@Import(
    ChatModelAvailabilityService::class,
    ChatModelRegistry::class,
    ChatModelsController::class,
    ChatModelsControllerTestConfiguration::class,
)
class ChatModelsControllerTestContext

@TestConfiguration
class ChatModelsControllerTestConfiguration {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun chatModelAvailabilityChecker(): ChatModelAvailabilityChecker =
        object : ChatModelAvailabilityChecker {
            override fun supports(model: ChatModelDescriptor): Boolean =
                model.id == "local-ollama-llama"

            override fun check(model: ChatModelDescriptor, apiTimeout: Duration): ChatModelAvailability =
                ChatModelAvailability.AVAILABLE
        }
}
