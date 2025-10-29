import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow.jar)
    alias(libs.plugins.jetbrains.dokka)
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
    // HTTP-Client stuff
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.neg)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.kotlinx.serialization.json)

    // UI stuff
    implementation(libs.formdev.flatlaf)
    implementation(libs.miglayout)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)

    // Project stuff
    implementation(projects.mazegameCommon)
    implementation(projects.mazegameClientKtor)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("de.dreamcube.mazegame.ui.UiControllerKt")
}

tasks.withType<ShadowJar> {
    archiveBaseName.set(project.name)
    mergeServiceFiles()
}

tasks.test {
    useJUnitPlatform()
}