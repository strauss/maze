import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow.jar)
    `maven-publish`
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":")))

    // --- Ktor ----------------------------------------------------------
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.content.neg)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.thymeleaf)

    // --- JWT -------------------------------------------------
    implementation(libs.jwt)

    // --- Jackson (YAML & Kotlin-Module) --------------------------------
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)

    implementation(projects.mazegameCommon)
    implementation(projects.mazegameClientKtor) // Used for client wrapper

    // --- Tests (optional) ---------------------------------------------
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("de.dreamcube.mazegame.server.ApplicationKt")
}

tasks.withType<ShadowJar> {
    archiveBaseName.set(project.name)
    mergeServiceFiles()
}

tasks.test { useJUnitPlatform() }
