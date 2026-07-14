package org.alterbit.aisme.modelcatalog

import java.time.Clock
import java.time.Duration
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
    classes = [ModelsControllerTestContext::class],
    properties = [
        "aisme.chat-models.local-ollama-llama.enabled=true",
        "aisme.chat-models.local-ollama-llama.display-order=10",
        "aisme.chat-models.local-ollama-llama.display-name=Local Ollama Llama",
        "aisme.chat-models.local-ollama-llama.description=Local Ollama model for chat requests.",
        "aisme.chat-models.local-ollama-llama.runtime.id=local-ollama",
        "aisme.chat-models.local-ollama-llama.runtime.model-name=llama3.2",
        "aisme.chat-models.cloud-gpt.enabled=true",
        "aisme.chat-models.cloud-gpt.display-order=20",
        "aisme.chat-models.cloud-gpt.display-name=Cloud GPT",
        "aisme.chat-models.cloud-gpt.description=Cloud model for online chat requests.",
        "aisme.chat-models.cloud-gpt.runtime.id=spring-ai",
        "aisme.chat-models.embedded-qwen-0-5b.enabled=false",
        "aisme.chat-models.embedded-qwen-1-5b.enabled=false",
        "aisme.chat-models.embedded-qwen-3b.enabled=false",
        "aisme.chat-models.embedded-mistral-7b.enabled=false",
        "aisme.chat-models.openai-compatible-example.enabled=false",
        "aisme.chat-models.hugging-face-tgi-example.enabled=false",
    ],
)
@AutoConfigureMockMvc
class ModelsControllerTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `lists configured chat models`() {
        mockMvc.get("/models")
            .andExpect {
                status { isOk() }
                jsonPath("$.models.length()") {
                    value(2)
                }
                jsonPath("$.models[0].id") {
                    value("local-ollama-llama")
                }
                jsonPath("$.models[0].displayName") {
                    value("Local Ollama Llama")
                }
                jsonPath("$.models[0].description") {
                    value("Local Ollama model for chat requests.")
                }
                jsonPath("$.models[0].runtime") {
                    value("OLLAMA")
                }
                jsonPath("$.models[0].mode") {
                    value("LOCAL_SERVER")
                }
                jsonPath("$.models[0].availability") {
                    value("AVAILABLE")
                }
                jsonPath("$.models[0].availableOffline") {
                    value(false)
                }
                jsonPath("$.models[0].promptsMayLeaveLocalMachine") {
                    value(false)
                }
                jsonPath("$.models[0].capabilities[0]") {
                    value("CHAT")
                }
                jsonPath("$.models[0].runtimeRequirements[0]") {
                    value("REQUIRES_OLLAMA_SERVER")
                }
                jsonPath("$.models[0].baseUrl") {
                    doesNotExist()
                }
                jsonPath("$.models[1].id") {
                    value("cloud-gpt")
                }
                jsonPath("$.models[1].description") {
                    value("Cloud model for online chat requests.")
                }
                jsonPath("$.models[1].availability") {
                    value("CONFIGURED")
                }
                jsonPath("$.models[1].promptsMayLeaveLocalMachine") {
                    value(true)
                }
                jsonPath("$.models[1].capabilities[0]") {
                    value("CHAT")
                }
                jsonPath("$.models[1].runtimeRequirements[0]") {
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
    ConfiguredChatModelsProperties::class,
)
@Import(
    ChatModelAvailabilityService::class,
    ChatModelRegistry::class,
    ModelsController::class,
    ModelsControllerTestConfiguration::class,
)
class ModelsControllerTestContext

@TestConfiguration
class ModelsControllerTestConfiguration {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun chatModelAvailabilityChecker(): ChatModelAvailabilityChecker =
        object : ChatModelAvailabilityChecker {
            override fun supports(model: ChatModelDescriptor): Boolean =
                model.id == "local-ollama-llama"

            override fun check(model: ChatModelDescriptor, timeout: Duration): ChatModelAvailability =
                ChatModelAvailability.AVAILABLE
        }
}
