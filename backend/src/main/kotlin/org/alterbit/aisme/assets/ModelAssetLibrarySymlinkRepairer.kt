package org.alterbit.aisme.assets

import java.nio.file.Files
import java.nio.file.Path

class ModelAssetLibrarySymlinkRepairer {
    fun repair(directory: Path) {
        val files = Files.list(directory).use { stream ->
            stream.filter(Files::isRegularFile).toList()
        }
        files
            .filter { file -> Files.size(file) == 0L }
            .forEach { alias ->
                val target = libraryAliasTarget(alias, files)
                if (target != null) {
                    Files.delete(alias)
                    Files.createSymbolicLink(alias, target.fileName)
                }
            }
    }

    private fun libraryAliasTarget(alias: Path, files: List<Path>): Path? {
        val prefixes = libraryAliasTargetPrefixes(alias.fileName.toString())
        return files
            .filter { candidate -> Files.size(candidate) > 0L }
            .filter { candidate -> prefixes.any { prefix -> candidate.fileName.toString().startsWith(prefix) } }
            .maxByOrNull { candidate -> candidate.fileName.toString().length }
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
