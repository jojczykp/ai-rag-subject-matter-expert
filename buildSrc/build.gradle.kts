import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

val rootGradleProperties = Properties().apply {
    file("../gradle.properties").inputStream().use(::load)
}
val jvmBytecodeTarget: String = requireNotNull(rootGradleProperties.getProperty("jvmBytecodeTarget")) {
    "Missing jvmBytecodeTarget in root gradle.properties"
}

java {
    sourceCompatibility = JavaVersion.toVersion(jvmBytecodeTarget)
    targetCompatibility = JavaVersion.toVersion(jvmBytecodeTarget)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jvmBytecodeTarget)
    }
}
