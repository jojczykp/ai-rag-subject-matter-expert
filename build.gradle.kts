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

// Backend coverage

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

val backendCheck = tasks.register("backendCheck") {
    group = "verification"
    description = "Runs backend tests and coverage verification."
    dependsOn(tasks.test, "koverVerify")
}

// Frontend build and verification

val npmExecutable = if (System.getProperty("os.name").lowercase().contains("windows")) {
    "npm.cmd"
} else {
    "npm"
}
val frontendDirectory = layout.projectDirectory.dir("frontend")
val frontendBuildDirectory = frontendDirectory.dir("dist")
val generatedFrontendResources = layout.buildDirectory.dir("generated/resources/frontend")

fun frontendNpmTask(
    name: String,
    npmScript: String,
    description: String,
): TaskProvider<Exec> =
    tasks.register<Exec>(name) {
        group = "frontend"
        this.description = description
        dependsOn("frontendInstall")
        workingDir = frontendDirectory.asFile
        commandLine(npmExecutable, "run", npmScript)

        inputs.file(frontendDirectory.file("package.json"))
        inputs.file(frontendDirectory.file("package-lock.json"))
        inputs.dir(frontendDirectory.dir("e2e"))
        inputs.dir(frontendDirectory.dir("src"))
        inputs.file(frontendDirectory.file("playwright.config.ts"))
        inputs.file(frontendDirectory.file("tsconfig.app.json"))
        inputs.file(frontendDirectory.file("tsconfig.json"))
        inputs.file(frontendDirectory.file("tsconfig.node.json"))
        inputs.file(frontendDirectory.file("vite.config.ts"))
    }

val frontendInstall = tasks.register<Exec>("frontendInstall") {
    group = "frontend"
    description = "Installs frontend dependencies from package-lock.json."
    workingDir = frontendDirectory.asFile
    commandLine(npmExecutable, "ci")

    inputs.file(frontendDirectory.file("package.json"))
    inputs.file(frontendDirectory.file("package-lock.json"))
    outputs.dir(frontendDirectory.dir("node_modules"))
}

val frontendBuild = tasks.register<Exec>("frontendBuild") {
    group = "frontend"
    description = "Builds the production frontend assets."
    dependsOn(frontendInstall)
    workingDir = frontendDirectory.asFile
    commandLine(npmExecutable, "run", "build")

    inputs.file(frontendDirectory.file("index.html"))
    inputs.file(frontendDirectory.file("package.json"))
    inputs.file(frontendDirectory.file("package-lock.json"))
    inputs.file(frontendDirectory.file("tsconfig.app.json"))
    inputs.file(frontendDirectory.file("tsconfig.json"))
    inputs.file(frontendDirectory.file("tsconfig.node.json"))
    inputs.file(frontendDirectory.file("vite.config.ts"))
    inputs.dir(frontendDirectory.dir("public"))
    inputs.dir(frontendDirectory.dir("src"))
    outputs.dir(frontendBuildDirectory)
}

val copyFrontendAssets = tasks.register<Copy>("copyFrontendAssets") {
    group = "frontend"
    description = "Copies frontend production assets into Spring Boot static resources."
    dependsOn(frontendBuild)

    from(frontendBuildDirectory)
    into(generatedFrontendResources.map { it.dir("static") })
}

tasks.processResources {
    dependsOn(copyFrontendAssets)
    from(generatedFrontendResources)
}

val frontendFormatCheck = frontendNpmTask(
    name = "frontendFormatCheck",
    npmScript = "format:check",
    description = "Checks frontend formatting.",
)

val frontendLint = frontendNpmTask(
    name = "frontendLint",
    npmScript = "lint",
    description = "Runs frontend linting.",
)

val frontendTestCoverage = frontendNpmTask(
    name = "frontendTestCoverage",
    npmScript = "test:coverage",
    description = "Runs frontend unit tests with coverage verification.",
)

val frontendTypecheck = frontendNpmTask(
    name = "frontendTypecheck",
    npmScript = "typecheck",
    description = "Runs frontend TypeScript checks.",
)

val frontendCheck = tasks.register("frontendCheck") {
    group = "verification"
    description = "Runs frontend format, lint, unit coverage, and typecheck verification."
    dependsOn(
        frontendFormatCheck,
        frontendLint,
        frontendTestCoverage,
        frontendTypecheck,
    )
}

tasks.register("backendBuild") {
    group = "build"
    description = "Builds backend application artifacts."
    dependsOn("bootJar", "jar")
}

// Verification

tasks.check {
    dependsOn(
        backendCheck,
        frontendCheck,
    )
}

tasks.build {
    dependsOn(
        "backendBuild",
        frontendBuild,
    )
}
