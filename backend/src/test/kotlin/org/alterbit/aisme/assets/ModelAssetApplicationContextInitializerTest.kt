package org.alterbit.aisme.assets

import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ModelAssetApplicationContextInitializerTest {
    @Test
    fun `downloads assets before singleton beans are instantiated`(@TempDir tempDirectory: Path) {
        val sourceFile = tempDirectory.resolve("source/model.onnx")
        val tokenizerSourceFile = tempDirectory.resolve("source/tokenizer.json")
        val modelFile = tempDirectory.resolve("models/model.onnx")
        val tokenizerFile = tempDirectory.resolve("models/tokenizer.json")
        Files.createDirectories(sourceFile.parent)
        Files.writeString(sourceFile, "onnx")
        Files.writeString(tokenizerSourceFile, "tokenizer")

        ApplicationContextRunner()
            .withInitializer(ModelAssetApplicationContextInitializer())
            .withBean(RequiresDownloadedAsset::class.java, Supplier { RequiresDownloadedAsset(modelFile) })
            .withPropertyValues(
                "aisme.embedding.runtimes.local-onnx.type=ONNX",
                "aisme.embedding.models.local-bge-small.enabled=true",
                "aisme.embedding.models.local-bge-small.download-missing-assets-on-startup=true",
                "aisme.embedding.models.local-bge-small.version=1.5",
                "aisme.embedding.models.local-bge-small.dimensions=384",
                "aisme.embedding.models.local-bge-small.assets[0].label=ONNX model",
                "aisme.embedding.models.local-bge-small.assets[0].path=$modelFile",
                "aisme.embedding.models.local-bge-small.assets[0].url=${sourceFile.toUri()}",
                "aisme.embedding.models.local-bge-small.assets[1].label=tokenizer",
                "aisme.embedding.models.local-bge-small.assets[1].path=$tokenizerFile",
                "aisme.embedding.models.local-bge-small.assets[1].url=${tokenizerSourceFile.toUri()}",
                "aisme.embedding.models.local-bge-small.runtime.id=local-onnx",
                "aisme.embedding.models.local-bge-small.runtime.model-path=$modelFile",
                "aisme.embedding.models.local-bge-small.runtime.tokenizer-path=$tokenizerFile",
                "aisme.chat.runtimes.local-ollama.type=OLLAMA",
                "aisme.chat.runtimes.local-ollama.base-url=http://localhost:11434",
                "aisme.chat.models.local-ollama-llama.enabled=true",
                "aisme.chat.models.local-ollama-llama.display-name=Local Ollama Llama",
                "aisme.chat.models.local-ollama-llama.runtime.id=local-ollama",
                "aisme.chat.models.local-ollama-llama.runtime.model-name=llama3.2",
            )
            .run { context ->
                context.getBean(RequiresDownloadedAsset::class.java).content shouldBe "onnx"
            }
    }

    @Test
    fun `downloads missing assets only for models with startup download enabled`(@TempDir tempDirectory: Path) {
        val sourceDirectory = tempDirectory.resolve("source")
        Files.createDirectories(sourceDirectory)
        val onnxSource = sourceDirectory.resolve("model.onnx")
        val tokenizerSource = sourceDirectory.resolve("tokenizer.json")
        val ggufSource = sourceDirectory.resolve("chat.gguf")
        Files.writeString(onnxSource, "onnx")
        Files.writeString(tokenizerSource, "tokenizer")
        Files.writeString(ggufSource, "gguf")

        val onnxTarget = tempDirectory.resolve("models/bge/model.onnx")
        val tokenizerTarget = tempDirectory.resolve("models/bge/tokenizer.json")
        val skippedOnnxTarget = tempDirectory.resolve("models/skipped/model.onnx")
        val skippedGgufTarget = tempDirectory.resolve("llama/models/skipped.gguf")
        val chatTarget = tempDirectory.resolve("llama/models/chat.gguf")

        ApplicationContextRunner()
            .withInitializer(ModelAssetApplicationContextInitializer())
            .withBean("marker", String::class.java, Supplier { "started" })
            .withPropertyValues(
                "aisme.embedding.runtimes.local-onnx.type=ONNX",
                "aisme.embedding.models.local-bge-small.enabled=true",
                "aisme.embedding.models.local-bge-small.download-missing-assets-on-startup=true",
                "aisme.embedding.models.local-bge-small.version=1.5",
                "aisme.embedding.models.local-bge-small.dimensions=384",
                "aisme.embedding.models.local-bge-small.assets[0].label=ONNX model",
                "aisme.embedding.models.local-bge-small.assets[0].path=$onnxTarget",
                "aisme.embedding.models.local-bge-small.assets[0].url=${onnxSource.toUri()}",
                "aisme.embedding.models.local-bge-small.assets[1].label=tokenizer",
                "aisme.embedding.models.local-bge-small.assets[1].path=$tokenizerTarget",
                "aisme.embedding.models.local-bge-small.assets[1].url=${tokenizerSource.toUri()}",
                "aisme.embedding.models.local-bge-small.runtime.id=local-onnx",
                "aisme.embedding.models.local-bge-small.runtime.model-path=$onnxTarget",
                "aisme.embedding.models.local-bge-small.runtime.tokenizer-path=$tokenizerTarget",
                "aisme.embedding.models.skipped-bge-small.enabled=true",
                "aisme.embedding.models.skipped-bge-small.download-missing-assets-on-startup=false",
                "aisme.embedding.models.skipped-bge-small.version=1.5",
                "aisme.embedding.models.skipped-bge-small.dimensions=384",
                "aisme.embedding.models.skipped-bge-small.assets[0].label=ONNX model",
                "aisme.embedding.models.skipped-bge-small.assets[0].path=$skippedOnnxTarget",
                "aisme.embedding.models.skipped-bge-small.runtime.id=local-onnx",
                "aisme.embedding.models.skipped-bge-small.runtime.model-path=$skippedOnnxTarget",
                "aisme.embedding.models.skipped-bge-small.runtime.tokenizer-path=${tempDirectory.resolve("models/skipped/tokenizer.json")}",
                "aisme.chat.runtimes.embedded-llama.type=EMBEDDED_LLAMA",
                "aisme.chat.runtimes.embedded-llama.asset-directory=${tempDirectory.resolve("llama")}",
                "aisme.chat.runtimes.embedded-llama.server-executable-path=${tempDirectory.resolve("llama/bin/llama-server")}",
                "aisme.chat.models.embedded-chat.enabled=true",
                "aisme.chat.models.embedded-chat.download-missing-assets-on-startup=true",
                "aisme.chat.models.embedded-chat.display-name=Embedded Chat",
                "aisme.chat.models.embedded-chat.assets[0].label=GGUF model",
                "aisme.chat.models.embedded-chat.assets[0].path=$chatTarget",
                "aisme.chat.models.embedded-chat.assets[0].url=${ggufSource.toUri()}",
                "aisme.chat.models.embedded-chat.runtime.id=embedded-llama",
                "aisme.chat.models.embedded-chat.runtime.model-name=chat",
                "aisme.chat.models.embedded-chat.runtime.gguf-file=models/chat.gguf",
                "aisme.chat.models.embedded-chat.runtime.context-size=2048",
                "aisme.chat.models.skipped-chat.enabled=true",
                "aisme.chat.models.skipped-chat.download-missing-assets-on-startup=false",
                "aisme.chat.models.skipped-chat.display-name=Skipped Chat",
                "aisme.chat.models.skipped-chat.assets[0].label=GGUF model",
                "aisme.chat.models.skipped-chat.assets[0].path=$skippedGgufTarget",
                "aisme.chat.models.skipped-chat.runtime.id=embedded-llama",
                "aisme.chat.models.skipped-chat.runtime.model-name=skipped",
                "aisme.chat.models.skipped-chat.runtime.gguf-file=models/skipped.gguf",
                "aisme.chat.models.skipped-chat.runtime.context-size=2048",
            )
            .run { context ->
                context.getBean("marker") shouldBe "started"
            }

        Files.readString(onnxTarget) shouldBe "onnx"
        Files.readString(tokenizerTarget) shouldBe "tokenizer"
        Files.readString(chatTarget) shouldBe "gguf"
        Files.exists(skippedOnnxTarget) shouldBe false
        Files.exists(skippedGgufTarget) shouldBe false
    }

    private class RequiresDownloadedAsset(
        path: Path,
    ) {
        val content: String = Files.readString(path)
    }
}
