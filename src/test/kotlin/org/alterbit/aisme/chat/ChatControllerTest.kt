package org.alterbit.aisme.chat

import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ConfiguredChatModelsProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest(
    classes = [ChatControllerTestContext::class],
    properties = [
        "aisme.chat-models[0].id=local-ollama-llama",
        "aisme.chat-models[0].display-name=Local Ollama Llama",
        "aisme.chat-models[0].runtime=OLLAMA",
        "aisme.chat-models[0].mode=LOCAL_SERVER",
        "aisme.chat-models[0].available-offline=false",
        "aisme.chat-models[0].base-url=http://localhost:11434",
    ],
)
@AutoConfigureMockMvc
class ChatControllerTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `answers chat request with selected model`() {
        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "local-ollama-llama",
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.modelId") {
                value("local-ollama-llama")
            }
            jsonPath("$.answer") {
                value("Fake answer for: How should I cook rice?")
            }
        }
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(ConfiguredChatModelsProperties::class)
@Import(
    AiChatService::class,
    ChatController::class,
    ChatModelRegistry::class,
    ChatControllerTestConfiguration::class,
)
class ChatControllerTestContext

@TestConfiguration
class ChatControllerTestConfiguration {
    @Bean
    fun aiModelClient(): AiModelClient =
        FakeAiModelClient(modelId = "local-ollama-llama")
}
