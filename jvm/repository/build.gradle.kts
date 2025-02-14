plugins {
    kotlin("jvm") version "1.9.25"
}

group = "pt.isel.daw"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain"))

    // To get the DI annotation
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    // To use Kotlin specific date and time functions
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
