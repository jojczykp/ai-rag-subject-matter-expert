plugins {
    base
}

subprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"
}

tasks.check {
    dependsOn(
        ":backend:check",
        ":frontend:check",
    )
}

tasks.build {
    dependsOn(
        ":backend:build",
        ":frontend:build",
    )
}

tasks.register("run") {
    group = "application"
    description = "Runs backend and frontend development servers. Use with --parallel."

    dependsOn(
        ":backend:run",
        ":frontend:run",
    )
}

gradle.taskGraph.whenReady {
    if (hasTask(":run") && !gradle.startParameter.isParallelProjectExecutionEnabled) {
        throw GradleException("Use './gradlew --parallel run' so backend and frontend can run at the same time.")
    }
}
