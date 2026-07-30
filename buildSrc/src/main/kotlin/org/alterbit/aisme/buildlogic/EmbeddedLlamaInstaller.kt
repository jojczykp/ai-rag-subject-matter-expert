package org.alterbit.aisme.buildlogic

import java.io.File
import java.net.URI
import java.nio.file.Files
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RelativePath

class EmbeddedLlamaInstaller(
    private val project: Project,
    private val serverDirectory: Directory,
    private val serverExecutable: RegularFile,
) {
    fun downloadAndInstall(
        archiveUrl: String,
        archiveFile: File,
        executableName: String,
        windows: Boolean,
    ) {
        downloadIfMissing(
            url = archiveUrl,
            targetFile = archiveFile,
        )
        install(
            archiveFile = archiveFile,
            executableName = executableName,
            windows = windows,
        )
    }

    fun install(
        archiveFile: File,
        executableName: String,
        windows: Boolean,
    ) {
        project.delete(serverDirectory)

        project.copy {
            from(
                if (windows) {
                    project.zipTree(archiveFile)
                } else {
                    project.tarTree(project.resources.gzip(archiveFile))
                },
            ) {
                include("**/$executableName")
                include("**/*.dylib")
                include("**/*.so")
                include("**/*.so.*")
                include("**/*.dll")
                eachFile {
                    relativePath = RelativePath(
                        true,
                        if (name == executableName) "llama-server" else name,
                    )
                }
                includeEmptyDirs = false
            }
            into(serverDirectory)
        }

        val executable = serverExecutable.asFile
        if (!windows) {
            executable.setExecutable(true)
            replaceEmptyLibraryAliasSymlinks(serverDirectory.asFile)
        }

        project.logger.lifecycle("Installed llama-server executable to ${executable.path}")
        verify()
    }

    private fun downloadIfMissing(
        url: String,
        targetFile: File,
    ) {
        if (!targetFile.isFile) {
            targetFile.parentFile.mkdirs()
            project.logger.lifecycle("Downloading llama-server archive from $url")
            URI(url).toURL().openStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            project.logger.lifecycle("llama-server archive already exists: ${targetFile.path}")
        }
    }

    fun verify() {
        val executable = serverExecutable.asFile
        require(executable.isFile) {
            "llama-server executable does not exist: ${executable.path}"
        }

        val process = ProcessBuilder(executable.path, "--version")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        val exitCode = process.waitFor()

        require(exitCode == 0) {
            "llama-server verification failed with exit code $exitCode: $output"
        }
        project.logger.lifecycle("Verified llama-server executable: $output")
    }

    private fun replaceEmptyLibraryAliasSymlinks(directory: File) {
        val files = directory.listFiles()?.filter { it.isFile } ?: emptyList()
        files
            .filter { it.length() == 0L }
            .forEach { alias ->
                val target = libraryAliasTarget(alias, files)
                if (target != null) {
                    Files.delete(alias.toPath())
                    Files.createSymbolicLink(alias.toPath(), target.toPath().fileName)
                }
            }
    }

    private fun libraryAliasTarget(alias: File, files: List<File>): File? {
        val prefixes = libraryAliasTargetPrefixes(alias.name)
        return files
            .filter { it.length() > 0L }
            .filter { candidate -> prefixes.any { prefix -> candidate.name.startsWith(prefix) } }
            .maxByOrNull { it.name.length }
    }

    private fun libraryAliasTargetPrefixes(fileName: String): List<String> =
        when {
            fileName.endsWith(".dylib") -> listOf(
                "${fileName.removeSuffix(".dylib")}.",
                "${fileName.removeSuffix(".0.dylib")}.",
            )

            fileName.endsWith(".so") -> listOf("$fileName.")
            fileName.contains(".so.") -> listOf("$fileName.")
            else -> emptyList()
        }
}
