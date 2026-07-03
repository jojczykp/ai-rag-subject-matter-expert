package org.alterbit.aisme.chat.ollama

import io.kotest.matchers.string.shouldContain
import java.time.Duration
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Tag("ollama")
@Testcontainers
class OllamaContainerSmokeTest {
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

    companion object {
        private const val OLLAMA_PORT = 11434

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
    }
}
