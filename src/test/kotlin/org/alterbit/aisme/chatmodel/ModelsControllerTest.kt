package org.alterbit.aisme.chatmodel

import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    classes = [ModelsControllerTestContext::class],
    properties = [
        "aisme.chat-models[0].id=local-ollama-llama",
        "aisme.chat-models[0].display-name=Local Ollama Llama",
        "aisme.chat-models[0].runtime=OLLAMA",
        "aisme.chat-models[0].mode=LOCAL_SERVER",
        "aisme.chat-models[0].available-offline=false",
        "aisme.chat-models[0].base-url=http://localhost:11434",
        "aisme.chat-models[1].id=cloud-gpt",
        "aisme.chat-models[1].display-name=Cloud GPT",
        "aisme.chat-models[1].runtime=SPRING_AI",
        "aisme.chat-models[1].mode=ONLINE",
        "aisme.chat-models[1].available-offline=false",
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
                    value("CONFIGURED")
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
                jsonPath("$.models[1].promptsMayLeaveLocalMachine") {
                    value(true)
                }
            }
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(ConfiguredChatModelsProperties::class)
@Import(
    ChatModelRegistry::class,
    ModelsController::class,
)
class ModelsControllerTestContext
