package org.alterbit.aisme.chatmodel

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
        "aisme.chat-models[0].id=local-ollama-llama",
        "aisme.chat-models[0].enabled=true",
        "aisme.chat-models[0].config.display-name=Local Ollama Llama",
        "aisme.chat-models[0].config.runtime=OLLAMA",
        "aisme.chat-models[0].config.mode=LOCAL_SERVER",
        "aisme.chat-models[0].config.available-offline=false",
        "aisme.chat-models[0].config.base-url=http://localhost:11434",
        "aisme.chat-models[0].config.model-name=llama3.2",
        "aisme.chat-models[1].id=cloud-gpt",
        "aisme.chat-models[1].enabled=true",
        "aisme.chat-models[1].config.display-name=Cloud GPT",
        "aisme.chat-models[1].config.runtime=SPRING_AI",
        "aisme.chat-models[1].config.mode=ONLINE",
        "aisme.chat-models[1].config.available-offline=false",
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
                jsonPath("$.models[0].baseUrl") {
                    doesNotExist()
                }
                jsonPath("$.models[1].id") {
                    value("cloud-gpt")
                }
                jsonPath("$.models[1].availability") {
                    value("CONFIGURED")
                }
                jsonPath("$.models[1].promptsMayLeaveLocalMachine") {
                    value(true)
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
