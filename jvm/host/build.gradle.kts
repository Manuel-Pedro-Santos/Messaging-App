plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version ("1.9.25")
    id("org.springframework.boot") version ("3.3.3")
    id("io.spring.dependency-management") version ("1.1.6")
}

group = "pt.isel.daw"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Module dependencies
    implementation(project(":http-api"))
    implementation(project(":domain"))
    implementation(project(":repository"))
    implementation(project(":repository-jdbi"))
    implementation(project(":http-pipeline"))
    implementation(project(":sse"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // for JDBI and Postgres
    implementation("org.jdbi:jdbi3-core:3.37.1")
    implementation("org.postgresql:postgresql:42.7.2")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
}

tasks.bootRun {
    mainClass = "pt.isel.daw.AppKt"
    //environment("DB_URL", "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres")
    //jdbc:postgresql://localhost:5433/postgres?user=postgres&password=1515
    environment("DB_URL", "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres")

}

tasks.withType<Test> {
    useJUnitPlatform()
    //environment("DB_URL", "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres")
    environment("DB_URL", "jdbc:postgresql://localhost:5433/postgres?user=postgres&password=1515")

    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}

task<Copy>("extractUberJar") {
    dependsOn("assemble")
    // opens the JAR containing everything...
    from(zipTree(layout.buildDirectory.file("libs/host-$version.jar").get().toString()))
    // ... into the 'build/dependency' folder
    into("build/dependency")
}
val dockerImageJvm = "jvm"
val dockerImageNginx = "nginx"
val dockerImagePostgresTest = "postgres-test"
val dockerImageUbuntu = "ubuntu"
task<Exec>("buildImageJvm") {
    dependsOn("extractUberJar")
    commandLine("docker", "build", "-t", dockerImageJvm, "-f", "test-infra/Dockerfile-jvm", ".")
}
task<Exec>("buildImageNginx") {
    commandLine("docker", "build", "-t", dockerImageNginx, "-f", "test-infra/Dockerfile-nginx", ".")
}
task<Exec>("buildImagePostgresTest") {
    commandLine(
        "docker",
        "build",
        "-t",
        dockerImagePostgresTest,
        "-f",
        "test-infra/Dockerfile-postgres-test",
        "../repository-jdbi",
    )
}
task<Exec>("buildImageUbuntu") {
    commandLine("docker", "build", "-t", dockerImageUbuntu, "-f", "test-infra/Dockerfile-ubuntu", ".")
}
task("buildImageAll") {
    dependsOn("buildImageJvm")
    dependsOn("buildImageNginx")
    dependsOn("buildImagePostgresTest")
    dependsOn("buildImageUbuntu")
}
task<Exec>("allUp") {
    commandLine("docker", "compose", "up", "--force-recreate", "-d")
}
task<Exec>("allDown") {
    commandLine("docker", "compose", "down")
}

kotlin {
    jvmToolchain(21)
}
