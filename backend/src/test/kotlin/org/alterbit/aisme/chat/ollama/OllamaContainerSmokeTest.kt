package org.alterbit.aisme.chat.ollama

import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.alterbit.aisme.api.ApiExceptionHandler
import org.alterbit.aisme.chat.AiChatService
import org.alterbit.aisme.chat.AiModelClients
import org.alterbit.aisme.chat.ChatController
import org.alterbit.aisme.chat.ChatContextRetriever
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityProperties
import org.alterbit.aisme.modelcatalog.ChatModelAvailabilityService
import org.alterbit.aisme.modelcatalog.ChatModelRegistry
import org.alterbit.aisme.modelcatalog.ChatModelsProperties
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.web.client.RestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Tag("ollama")
@Testcontainers
@SpringBootTest(classes = [OllamaContainerTestContext::class])
@AutoConfigureMockMvc
class OllamaContainerSmokeTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `starts ollama container and responds to tags api`() {
        val baseUrl = "http://${ollama.host}:${ollama.getMappedPort(OLLAMA_PORT)}"

        val response = RestClient.create(baseUrl)
            .get()
            .uri("/api/tags")
            .retrieve()
            .body(String::class.java)

        response shouldContain "\"models\""
    }

    @Test
    fun `routes application chat request through ollama model`() {
        pullConfiguredModel()

        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "container-ollama",
                  "message": "Reply with one short sentence about rice."
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.modelId") {
                value("container-ollama")
            }
            jsonPath("$.answer") {
                isNotEmpty()
            }
        }
    }

    private fun pullConfiguredModel() {
        val result = ollama.execInContainer("ollama", "pull", testModelName)
        check(result.exitCode == 0) {
            "Failed to pull Ollama test model '$testModelName': ${result.stderr}${result.stdout}"
        }
    }

    companion object {
        private const val OLLAMA_PORT = 11434

        private val testModelName: String = System.getProperty(
            "aisme.ollama.test.model",
            "tinyllama:latest",
        )

        private val imageName: DockerImageName = DockerImageName.parse(
            System.getProperty("aisme.ollama.test.image", "ollama/ollama:latest"),
        )

        @Container
        @JvmStatic
        val ollama: GenericContainer<*> = GenericContainer(imageName)
            .withExposedPorts(OLLAMA_PORT)
            .waitingFor(
                Wait.forHttp("/api/tags")
                    .forPort(OLLAMA_PORT)
                    .forStatusCode(200),
            )
            .withStartupTimeout(Duration.ofMinutes(2))

        @JvmStatic
        @DynamicPropertySource
        fun ollamaProperties(registry: DynamicPropertyRegistry) {
            registry.add("aisme.chat.runtimes.local-ollama.type") { "OLLAMA" }
            registry.add("aisme.chat.runtimes.local-ollama.base-url") {
                "http://${ollama.host}:${ollama.getMappedPort(OLLAMA_PORT)}"
            }
            registry.add("aisme.chat.models.container-ollama.enabled") { "true" }
            registry.add("aisme.chat.models.container-ollama.display-name") { "Container Ollama" }
            registry.add("aisme.chat.models.container-ollama.runtime.id") { "local-ollama" }
            registry.add("aisme.chat.models.container-ollama.runtime.model-name") { testModelName }
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
    ChatProperties::class,
    ChatModelAvailabilityProperties::class,
    ChatModelsProperties::class,
)
@Import(
    AiChatService::class,
    AiModelClients::class,
    ApiExceptionHandler::class,
    ChatController::class,
    ChatModelAvailabilityService::class,
    ChatModelRegistry::class,
    OllamaAiModelClientProvider::class,
    OllamaModelAvailabilityChecker::class,
    SpringAiOllamaChatApiFactory::class,
)
class OllamaContainerTestContext {
    @Bean
    fun clock(): java.time.Clock =
        java.time.Clock.systemUTC()

    @Bean
    fun chatContextRetriever(): ChatContextRetriever =
        ChatContextRetriever { _, _ -> emptyList() }
}
