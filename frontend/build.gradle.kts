plugins {
    base
}

val npmExecutable = if (System.getProperty("os.name").lowercase().contains("windows")) {
    "npm.cmd"
} else {
    "npm"
}

fun npmScriptTask(
    name: String,
    npmScript: String,
    description: String,
): TaskProvider<Exec> =
    tasks.register<Exec>(name) {
        group = "frontend"
        this.description = description
        dependsOn("npmInstall")
        workingDir = layout.projectDirectory.asFile
        commandLine(npmExecutable, "run", npmScript)

        inputs.file(layout.projectDirectory.file("package.json"))
        inputs.file(layout.projectDirectory.file("package-lock.json"))
        inputs.dir(layout.projectDirectory.dir("e2e"))
        inputs.dir(layout.projectDirectory.dir("src"))
        inputs.file(layout.projectDirectory.file("playwright.config.ts"))
        inputs.file(layout.projectDirectory.file("tsconfig.app.json"))
        inputs.file(layout.projectDirectory.file("tsconfig.json"))
        inputs.file(layout.projectDirectory.file("tsconfig.node.json"))
        inputs.file(layout.projectDirectory.file("vite.config.ts"))
    }

val npmInstall = tasks.register<Exec>("npmInstall") {
    group = "frontend"
    description = "Installs frontend dependencies from package-lock.json."
    workingDir = layout.projectDirectory.asFile
    commandLine(npmExecutable, "ci")

    inputs.file(layout.projectDirectory.file("package.json"))
    inputs.file(layout.projectDirectory.file("package-lock.json"))
    outputs.dir(layout.projectDirectory.dir("node_modules"))
}

val npmBuild = tasks.register<Exec>("npmBuild") {
    group = "frontend"
    description = "Builds the production frontend assets."
    dependsOn(npmInstall)
    workingDir = layout.projectDirectory.asFile
    commandLine(npmExecutable, "run", "build")

    inputs.file(layout.projectDirectory.file("index.html"))
    inputs.file(layout.projectDirectory.file("package.json"))
    inputs.file(layout.projectDirectory.file("package-lock.json"))
    inputs.file(layout.projectDirectory.file("tsconfig.app.json"))
    inputs.file(layout.projectDirectory.file("tsconfig.json"))
    inputs.file(layout.projectDirectory.file("tsconfig.node.json"))
    inputs.file(layout.projectDirectory.file("vite.config.ts"))
    inputs.dir(layout.projectDirectory.dir("public"))
    inputs.dir(layout.projectDirectory.dir("src"))
    outputs.dir(layout.projectDirectory.dir("dist"))
}

tasks.register<Exec>("run") {
    group = "application"
    description = "Runs the frontend Vite development server."
    dependsOn(npmInstall)
    workingDir = layout.projectDirectory.asFile
    commandLine(npmExecutable, "run", "dev")
}

val formatCheck = npmScriptTask(
    name = "formatCheck",
    npmScript = "format:check",
    description = "Checks frontend formatting.",
)

val lint = npmScriptTask(
    name = "lint",
    npmScript = "lint",
    description = "Runs frontend linting.",
)

val testCoverage = npmScriptTask(
    name = "testCoverage",
    npmScript = "test:coverage",
    description = "Runs frontend unit tests with coverage verification.",
)

val typecheck = npmScriptTask(
    name = "typecheck",
    npmScript = "typecheck",
    description = "Runs frontend TypeScript checks.",
)

tasks.assemble {
    dependsOn(npmBuild)
}

tasks.check {
    dependsOn(
        formatCheck,
        lint,
        testCoverage,
        typecheck,
    )
}
