import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("aisme.embedding-model")
    id("aisme.embedded-llama")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kover)
}

val javaToolchainVersion = providers.gradleProperty("javaToolchainVersion").get().toInt()
val jvmBytecodeTarget = providers.gradleProperty("jvmBytecodeTarget").get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaToolchainVersion)
    }
    sourceCompatibility = JavaVersion.toVersion(jvmBytecodeTarget)
    targetCompatibility = JavaVersion.toVersion(jvmBytecodeTarget)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.fromTarget(jvmBytecodeTarget)
    }
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.ai.bom.get().toString())
    }
}

dependencies {
    runtimeOnly(libs.postgresql)

    implementation(libs.commons.compress)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.hf.tokenizers)
    implementation(libs.onnxruntime)
    implementation(libs.spring.ai.starter.model.ollama)
    implementation(libs.spring.boot.flyway)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("ollama", "openai-compatible", "hugging-face-tgi", "onnx-model")
    }
}

tasks.register<Test>("onnxModelTest") {
    group = "verification"
    description = "Runs optional real ONNX embedding model tests."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.test)
    onlyIf {
        gradle.startParameter.taskNames.any { it == "onnxModelTest" || it.endsWith(":onnxModelTest") }
    }

    useJUnitPlatform {
        includeTags("onnx-model")
    }
}

tasks.register<Test>("openAiCompatibleTest") {
    group = "verification"
    description = "Runs optional OpenAI-compatible adapter flow tests."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.test)
    onlyIf {
        gradle.startParameter.taskNames.any { it == "openAiCompatibleTest" || it.endsWith(":openAiCompatibleTest") }
    }

    useJUnitPlatform {
        includeTags("openai-compatible")
    }
}

tasks.register<Test>("ollamaTest") {
    group = "verification"
    description = "Runs optional Ollama Testcontainers tests."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.test)
    onlyIf {
        gradle.startParameter.taskNames.any { it == "ollamaTest" || it.endsWith(":ollamaTest") }
    }

    useJUnitPlatform {
        includeTags("ollama")
    }
}

tasks.register<Test>("huggingFaceTgiTest") {
    group = "verification"
    description = "Runs optional Hugging Face TGI adapter flow tests."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.test)
    onlyIf {
        gradle.startParameter.taskNames.any { it == "huggingFaceTgiTest" || it.endsWith(":huggingFaceTgiTest") }
    }

    useJUnitPlatform {
        includeTags("hugging-face-tgi")
    }
}

tasks.register("extendedIntegrationTest") {
    group = "verification"
    description = "Runs optional backend integration tests that are excluded from default verification."
    dependsOn(
        "ollamaTest",
        "openAiCompatibleTest",
        "huggingFaceTgiTest",
        "onnxModelTest",
    )
}

tasks.bootRun {
    workingDir = projectDir
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.register<BootRun>("run") {
    group = "application"
    description = "Runs the backend Spring Boot application."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(tasks.bootRun.flatMap { it.mainClass })
    workingDir = projectDir
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.register("cleanDownloadedModelAssets") {
    group = "model management"
    description = "Deletes locally downloaded model and runtime assets so startup can download them again."
    dependsOn(
        "cleanLocalEmbeddingModelAssets",
        "cleanEmbeddedLlamaModel",
        "cleanEmbeddedLlamaServer",
    )
}

kover {
    reports {
        filters {
            excludes {
                classes("org.alterbit.aisme.AismeApplicationKt")
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

tasks.check {
    dependsOn("koverVerify")
}
