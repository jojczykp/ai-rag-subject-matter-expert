import java.net.URI

val releaseVersion = "b9892"
val releaseBaseUrl = "https://github.com/ggml-org/llama.cpp/releases/download/$releaseVersion"

val assetDirectory = layout.projectDirectory.dir("models/llama")
val modelPath = assetDirectory.file("models/llama.gguf")

val serverDirectory = assetDirectory.dir("bin")
val serverExecutablePath = assetDirectory.file("bin/llama-server")

val exampleLlamaModelUrl =
    "https://huggingface.co/QuantFactory/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/TinyLlama-1.1B-Chat-v1.0.Q4_K_M.gguf"

val serverArchivesDirectory = layout.buildDirectory.dir("embedded-llama-server")

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
            if (!targetFile.isFile) {
                targetFile.parentFile.mkdirs()
                val archiveUrl = "$releaseBaseUrl/${distribution.archiveName}"
                logger.lifecycle("Downloading llama-server archive from $archiveUrl")
                URI(archiveUrl).toURL().openStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                logger.lifecycle("llama-server archive already exists: ${targetFile.path}")
            }

            copy {
                from(
                    if (distribution.windows) {
                        zipTree(targetFile)
                    } else {
                        tarTree(resources.gzip(targetFile))
                    },
                ) {
                    include("**/${distribution.executableName}")
                    eachFile {
                        relativePath = RelativePath(true, "llama-server")
                    }
                    includeEmptyDirs = false
                }
                into(serverDirectory)
            }

            val executable = serverExecutablePath.asFile
            if (!distribution.windows) {
                executable.setExecutable(true)
            }
            logger.lifecycle("Installed llama-server executable to ${executable.path}")
        }
    }
}

serverDistributions.values.forEach(::registerLlamaServerDownloadTask)

tasks.register("embeddedLlamaDownloadServer") {
    group = "model management"
    description = "Downloads the llama-server archive for the current platform and installs it under backend/models."
    dependsOn(provider { currentLlamaServerDistribution().taskName })
}

tasks.register("embeddedLlamaDownloadModel") {
    group = "model management"
    description = "Downloads the embedded llama GGUF model asset when it is missing."

    doLast {
        val modelFile = modelPath.asFile
        if (modelFile.isFile) {
            logger.lifecycle("embedded llama model already exists: ${modelFile.path}")
            return@doLast
        }

        modelFile.parentFile.mkdirs()
        logger.lifecycle("Downloading embedded llama model to ${modelFile.path}")
        URI(exampleLlamaModelUrl).toURL().openStream().use { input ->
            modelFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

tasks.register<Delete>("cleanEmbeddedLlama") {
    group = "model management"
    description = "Deletes locally downloaded embedded llama assets under backend/models/llama."
    delete(assetDirectory)
}
