import java.net.URI
import org.alterbit.aisme.buildlogic.EmbeddedLlamaInstaller

val releaseVersion = "b9892"
val releaseBaseUrl = "https://github.com/ggml-org/llama.cpp/releases/download/$releaseVersion"

val assetDirectory = layout.projectDirectory.dir("models/llama")

val serverDirectory = assetDirectory.dir("bin")
val serverExecutablePath = assetDirectory.file("bin/llama-server")

val serverArchivesDirectory = layout.buildDirectory.dir("embedded-llama-server")
val embeddedLlamaInstaller = EmbeddedLlamaInstaller(
    project = project,
    serverDirectory = serverDirectory,
    serverExecutable = serverExecutablePath,
)

data class EmbeddedLlamaModelAsset(
    val taskName: String,
    val displayName: String,
    val fileName: String,
    val url: String,
)

data class LlamaServerDistribution(
    val taskName: String,
    val classifier: String,
    val archiveName: String,
    val executableName: String,
    val windows: Boolean = false,
)

val serverDistributions = mapOf(
    "macAppleSilicon" to LlamaServerDistribution(
        taskName = "embeddedLlamaDownloadServerMacAppleSilicon",
        classifier = "macOS Apple Silicon",
        archiveName = "llama-$releaseVersion-bin-macos-arm64.tar.gz",
        executableName = "llama-server",
    ),
    "macIntel" to LlamaServerDistribution(
        taskName = "embeddedLlamaDownloadServerMacIntel",
        classifier = "macOS Intel x64",
        archiveName = "llama-$releaseVersion-bin-macos-x64.tar.gz",
        executableName = "llama-server",
    ),
    "linuxUbuntuX64" to LlamaServerDistribution(
        taskName = "embeddedLlamaDownloadServerLinuxUbuntuX64",
        classifier = "Linux Ubuntu x64",
        archiveName = "llama-$releaseVersion-bin-ubuntu-x64.tar.gz",
        executableName = "llama-server",
    ),
    "windowsX64" to LlamaServerDistribution(
        taskName = "embeddedLlamaDownloadServerWindowsX64",
        classifier = "Windows x64",
        archiveName = "llama-$releaseVersion-bin-win-cpu-x64.zip",
        executableName = "llama-server.exe",
        windows = true,
    ),
)

val embeddedModels = listOf(
    EmbeddedLlamaModelAsset(
        taskName = "embeddedLlamaDownloadQwen0p5BModel",
        displayName = "Qwen2.5 0.5B Instruct Q4_K_M",
        fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
        url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
    ),
    EmbeddedLlamaModelAsset(
        taskName = "embeddedLlamaDownloadQwen1p5BModel",
        displayName = "Qwen2.5 1.5B Instruct Q4_K_M",
        fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
    ),
)

fun currentLlamaServerDistribution(): LlamaServerDistribution {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()

    return when {
        os.contains("mac") && (arch == "aarch64" || arch == "arm64") -> serverDistributions["macAppleSilicon"]
        os.contains("mac") && (arch == "x86_64" || arch == "amd64") -> serverDistributions["macIntel"]
        os.contains("linux") && (arch == "x86_64" || arch == "amd64") -> serverDistributions["linuxUbuntuX64"]
        os.contains("windows") && (arch == "x86_64" || arch == "amd64") -> serverDistributions["windowsX64"]
        else -> null
    } ?: error("Unsupported platform for automatic llama-server download: os.name=$os, os.arch=$arch")
}

fun registerLlamaServerDownloadTask(distribution: LlamaServerDistribution) {
    val archiveFile = serverArchivesDirectory.map { directory -> directory.file(distribution.archiveName) }

    tasks.register(distribution.taskName) {
        group = "model management"
        description = "Downloads and installs ${distribution.classifier} llama-server under backend/models."
        doLast {
            val targetFile = archiveFile.get().asFile
            embeddedLlamaInstaller.downloadAndInstall(
                archiveUrl = "$releaseBaseUrl/${distribution.archiveName}",
                archiveFile = targetFile,
                executableName = distribution.executableName,
                windows = distribution.windows,
            )
        }
    }
}

fun registerEmbeddedModelDownloadTask(modelAsset: EmbeddedLlamaModelAsset) {
    val modelPath = assetDirectory.file("models/${modelAsset.fileName}")

    tasks.register(modelAsset.taskName) {
        group = "model management"
        description = "Downloads the embedded ${modelAsset.displayName} GGUF model asset when it is missing."

        doLast {
            val modelFile = modelPath.asFile
            if (modelFile.isFile) {
                logger.lifecycle("embedded ${modelAsset.displayName} model already exists: ${modelFile.path}")
                return@doLast
            }

            modelFile.parentFile.mkdirs()
            logger.lifecycle("Downloading embedded ${modelAsset.displayName} model to ${modelFile.path}")
            URI(modelAsset.url).toURL().openStream().use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

serverDistributions.values.forEach(::registerLlamaServerDownloadTask)
embeddedModels.forEach(::registerEmbeddedModelDownloadTask)

tasks.register("embeddedLlamaDownloadServer") {
    group = "model management"
    description = "Downloads the llama-server archive for the current platform and installs it under backend/models."
    dependsOn(provider { currentLlamaServerDistribution().taskName })
}

tasks.register("embeddedLlamaVerifyServer") {
    group = "model management"
    description = "Verifies that the installed llama-server executable can start."
    doLast {
        embeddedLlamaInstaller.verify()
    }
}

tasks.register("embeddedLlamaDownloadModel") {
    group = "model management"
    description = "Downloads all embedded Qwen GGUF model assets when they are missing."
    dependsOn(embeddedModels.map { modelAsset -> modelAsset.taskName })
}

tasks.register<Delete>("cleanEmbeddedLlamaModel") {
    group = "model management"
    description = "Deletes the locally downloaded embedded Qwen GGUF models."
    delete(embeddedModels.map { modelAsset -> assetDirectory.file("models/${modelAsset.fileName}") })
}

tasks.register<Delete>("cleanEmbeddedLlamaServer") {
    group = "model management"
    description = "Deletes the locally installed embedded llama server and cached server archives."
    delete(serverDirectory)
    delete(serverArchivesDirectory)
}
