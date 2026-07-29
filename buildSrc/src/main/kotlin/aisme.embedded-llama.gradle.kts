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
            embeddedLlamaInstaller.downloadAndInstall(
                archiveUrl = "$releaseBaseUrl/${distribution.archiveName}",
                archiveFile = targetFile,
                executableName = distribution.executableName,
                windows = distribution.windows,
            )
        }
    }
}

serverDistributions.values.forEach(::registerLlamaServerDownloadTask)

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

tasks.register<Delete>("cleanEmbeddedLlamaModel") {
    group = "model management"
    description = "Deletes the locally downloaded embedded GGUF models."
    delete(assetDirectory.dir("models"))
}

tasks.register<Delete>("cleanEmbeddedLlamaServer") {
    group = "model management"
    description = "Deletes the locally installed embedded llama server and cached server archives."
    delete(serverDirectory)
    delete(serverArchivesDirectory)
}
