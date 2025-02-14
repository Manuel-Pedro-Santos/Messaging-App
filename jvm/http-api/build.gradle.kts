plugins {
    kotlin("jvm") version "1.9.25"
    id("com.adarshr.test-logger") version "4.0.0"
}

group = "pt.isel.daw"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":services"))

    // To use Spring MVC
    implementation("org.springframework:spring-webmvc:6.1.13")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    // for JDBI and Postgres Tests
    testImplementation(project(":repository-jdbi"))
    testImplementation("org.jdbi:jdbi3-core:3.37.1")
    testImplementation("org.postgresql:postgresql:42.7.2")

    // To use WebTestClient on tests
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    environment("DB_URL", "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres")
    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}
