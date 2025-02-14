plugins {
    kotlin("jvm") version "1.9.25"
    id("com.adarshr.test-logger") version "4.0.0"
}

group = "pt.isel.daw"
version = "0.1.0.SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":domain"))
    api(project(":repository"))

    // To get the DI annotation
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // To use Kotlin specific date and time functions
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // To use email
    implementation("javax.mail:javax.mail-api:1.6.2")
    implementation("com.sun.mail:javax.mail:1.6.2")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    // for JDBI
    testImplementation(project(":repository-jdbi"))
    testImplementation("org.jdbi:jdbi3-core:3.37.1")
    testImplementation("org.postgresql:postgresql:42.7.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

tasks.test {
    useJUnitPlatform()
    environment("DB_URL", "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres")
    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}

kotlin {
    jvmToolchain(21)
}
