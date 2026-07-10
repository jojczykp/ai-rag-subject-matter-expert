package org.alterbit.aisme.chat.embedded

import io.kotest.matchers.collections.shouldContainExactly
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import org.alterbit.aisme.api.ApiExceptionHandler
import org.alterbit.aisme.chat.AiChatService
import org.alterbit.aisme.chat.AiModelClients
import org.alterbit.aisme.chat.ChatController
import org.alterbit.aisme.chat.ChatProperties
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityProperties
import org.alterbit.aisme.chatmodel.ChatModelAvailabilityService
import org.alterbit.aisme.chatmodel.ChatModelRegistry
import org.alterbit.aisme.chatmodel.ConfiguredChatModelsProperties
import org.alterbit.aisme.chatmodel.EmbeddedOfflineModelAvailabilityChecker
import org.alterbit.aisme.chatmodel.ModelsController
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
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [EmbeddedOfflineStartupIntegrationTestContext::class])
@AutoConfigureMockMvc
class EmbeddedOfflineStartupIntegrationTest(
    private val mockMvc: MockMvc,
    private val processLauncher: FakeLlamaRuntimeProcessLauncher,
) {
    @Test
    fun `starts embedded runtime with fakes and exposes model as available`() {
        processLauncher.commands.map { it.take(9) } shouldContainExactly listOf(
            listOf(
                STARTUP_TEST_ASSETS.serverExecutable.toString(),
                "--host",
                "127.0.0.1",
                "--port",
                "19001",
                "--model",
                STARTUP_TEST_ASSETS.ggufFile.toString(),
                "--ctx-size",
                "4096",
            ),
        )

        mockMvc.get("/models")
            .andExpect {
                status { isOk() }
                jsonPath("$.models.length()") {
                    value(1)
                }
                jsonPath("$.models[0].id") {
                    value("embedded-startup")
                }
                jsonPath("$.models[0].availability") {
                    value("AVAILABLE")
                }
                jsonPath("$.models[0].promptsMayLeaveLocalMachine") {
                    value(false)
                }
            }

        mockMvc.post("/chat") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "modelId": "embedded-startup",
                  "message": "How should I cook rice?"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.modelId") {
                value("embedded-startup")
            }
            jsonPath("$.answer") {
                value("Fake embedded startup answer")
            }
        }
    }

    companion object {
        private val STARTUP_TEST_ASSETS = StartupTestAssets.create()

        @JvmStatic
        @DynamicPropertySource
        fun embeddedRuntimeProperties(registry: DynamicPropertyRegistry) {
            registry.add("aisme.llama-runtime.enabled") { "true" }
            registry.add("aisme.llama-runtime.config.asset-directory") {
                STARTUP_TEST_ASSETS.assetDirectory.toString()
            }
            registry.add("aisme.llama-runtime.config.server-executable-path") {
                STARTUP_TEST_ASSETS.serverExecutable.toString()
            }
            registry.add("aisme.llama-runtime.config.models[0].id") { "embedded-startup" }
            registry.add("aisme.llama-runtime.config.models[0].display-name") { "Embedded Startup" }
            registry.add("aisme.llama-runtime.config.models[0].gguf-file") {
                STARTUP_TEST_ASSETS.ggufFile.fileName.toString()
            }
            registry.add("aisme.llama-runtime.config.models[0].context-size") { "4096" }
            registry.add("aisme.chat-models[0].id") { "embedded-startup" }
            registry.add("aisme.chat-models[0].enabled") { "true" }
            registry.add("aisme.chat-models[0].config.display-name") { "Embedded Startup" }
            registry.add("aisme.chat-models[0].config.runtime") { "EMBEDDED_OFFLINE" }
            registry.add("aisme.chat-models[0].config.mode") { "EMBEDDED_OFFLINE" }
            registry.add("aisme.chat-models[0].config.available-offline") { "true" }
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
    ConfiguredChatModelsProperties::class,
    LlamaRuntimeProperties::class,
)
@Import(
    AiChatService::class,
    AiModelClients::class,
    ApiExceptionHandler::class,
    ChatController::class,
    ChatModelAvailabilityService::class,
    ChatModelRegistry::class,
    EmbeddedOfflineModelAvailabilityChecker::class,
    LlamaRuntimeAiModelClientProvider::class,
    LlamaRuntimeProcessManager::class,
    ModelsController::class,
    EmbeddedOfflineStartupIntegrationTestConfiguration::class,
)
class EmbeddedOfflineStartupIntegrationTestContext

@TestConfiguration
class EmbeddedOfflineStartupIntegrationTestConfiguration {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun portAllocator(): EphemeralLlamaRuntimePortAllocator =
        EphemeralLlamaRuntimePortAllocator { 19001 }

    @Bean
    fun processLauncher(): FakeLlamaRuntimeProcessLauncher =
        FakeLlamaRuntimeProcessLauncher()

    @Bean
    fun readinessProbe(): LlamaServerReadinessProbe =
        LlamaServerReadinessProbe { _, _ -> true }

    @Bean
    fun processOutputLogger(): LlamaRuntimeProcessOutputLogger =
        LlamaRuntimeProcessOutputLogger(lineConsumer = { _, _, _ -> })

    @Bean
    fun llamaServerChatApiFactory(): LlamaServerChatApiFactory =
        object : LlamaServerChatApiFactory {
            override fun create(baseUrl: String, timeout: Duration): LlamaServerChatApi =
                object : LlamaServerChatApi {
                    override fun chat(request: LlamaServerChatRequest): LlamaServerChatResponse =
                        LlamaServerChatResponse(
                            choices = listOf(
                                LlamaServerChatChoice(
                                    message = LlamaServerChatMessage(
                                        role = "assistant",
                                        content = "Fake embedded startup answer",
                                    ),
                                ),
                            ),
                        )
                }
        }
}

class FakeLlamaRuntimeProcessLauncher : LlamaRuntimeProcessLauncher {
    val commands = mutableListOf<List<String>>()

    override fun start(command: List<String>): Process {
        commands += command
        return FakeProcess()
    }
}

private class FakeProcess : Process() {
    override fun getOutputStream(): OutputStream =
        ByteArrayOutputStream()

    override fun getInputStream(): InputStream =
        ByteArrayInputStream(ByteArray(0))

    override fun getErrorStream(): InputStream =
        ByteArrayInputStream(ByteArray(0))

    override fun waitFor(): Int =
        0

    override fun exitValue(): Int =
        0

    override fun destroy() = Unit
}

private data class StartupTestAssets(
    val assetDirectory: Path,
    val ggufFile: Path,
    val serverExecutable: Path,
) {
    companion object {
        fun create(): StartupTestAssets {
            val assetDirectory = Files.createTempDirectory("aisme-llama-startup-assets-")
            val ggufFile = Files.createFile(assetDirectory.resolve("model.gguf"))
            Files.writeString(ggufFile, "test model")
            val serverExecutable = Files.createTempFile("aisme-llama-server-", "")
            serverExecutable.toFile().setExecutable(true)
            return StartupTestAssets(
                assetDirectory = assetDirectory,
                ggufFile = ggufFile,
                serverExecutable = serverExecutable,
            )
        }
    }
}
