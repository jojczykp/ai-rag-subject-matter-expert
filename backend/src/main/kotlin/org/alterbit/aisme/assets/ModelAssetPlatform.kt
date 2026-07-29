package org.alterbit.aisme.assets

data class ModelAssetPlatform(
    val os: ModelAssetOperatingSystem,
    val arch: ModelAssetArchitecture,
) {
    companion object {
        fun current(): ModelAssetPlatform =
            ModelAssetPlatform(
                os = normalizedOs(System.getProperty("os.name")),
                arch = normalizedArch(System.getProperty("os.arch")),
            )

        private fun normalizedOs(value: String): ModelAssetOperatingSystem {
            val normalized = value.lowercase()
            return when {
                normalized.contains("mac") -> ModelAssetOperatingSystem.MACOS
                normalized.contains("linux") -> ModelAssetOperatingSystem.LINUX
                normalized.contains("windows") -> ModelAssetOperatingSystem.WINDOWS
                else -> error("Unsupported model asset operating system: $value")
            }
        }

        private fun normalizedArch(value: String): ModelAssetArchitecture {
            val normalized = value.lowercase()
            return when (normalized) {
                "aarch64", "arm64" -> ModelAssetArchitecture.AARCH64
                "x86_64", "amd64" -> ModelAssetArchitecture.X86_64
                else -> error("Unsupported model asset architecture: $value")
            }
        }
    }
}

enum class ModelAssetOperatingSystem {
    MACOS,
    LINUX,
    WINDOWS,
}

enum class ModelAssetArchitecture {
    AARCH64,
    X86_64,
}

fun ModelAsset.matches(platform: ModelAssetPlatform): Boolean =
    matchesSelector(os, platform.os) && matchesSelector(arch, platform.arch)

private fun <T : Enum<T>> matchesSelector(
    configuredValue: T?,
    actualValue: T,
): Boolean =
    configuredValue == null || configuredValue == actualValue
