plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kover)
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.ai.bom.get().toString())
    }
}

dependencies {
    runtimeOnly(libs.postgresql)

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
        excludeTags("ollama", "openai-compatible")
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

tasks.bootRun {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
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
