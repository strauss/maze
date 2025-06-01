import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow.jar)
}

group   = "de.dreamcube"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()   // zieht den alten Java-Client aus der lokalen Maven-Repo
}

dependencies {
    implementation(platform(project(":")))        // <dependencyManagement> der Basis

    // --- Ktor ----------------------------------------------------------
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.content.neg)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.thymeleaf)

    // --- Logging & JWT -------------------------------------------------
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.jwt)

    // --- Jackson (YAML & Kotlin-Module) --------------------------------
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)

    // --- Abhängigkeit auf alten Client --------------------------------
    implementation("mazegame:client:1.0-SNAPSHOT")   // kommt aus mavenLocal() oder Repo

    // --- Tests (optional) ---------------------------------------------
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("mazegame.server_ktor.ApplicationKt")   // wie im Shade-Plugin der POM :contentReference[oaicite:5]{index=5}
}

tasks.withType<ShadowJar> {
    archiveBaseName.set(project.name)
    mergeServiceFiles()
}

tasks.test { useJUnitPlatform() }   // funktioniert, falls JUnit wieder aktiviert wird
