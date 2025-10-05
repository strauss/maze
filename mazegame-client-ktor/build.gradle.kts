import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow.jar)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":")))        // <dependencyManagement> der Basis

    // Ktor
    // implementation(libs.ktor.client.core)
    // implementation(libs.ktor.client.cio)
    implementation(libs.ktor.network)
    implementation(libs.ktor.utils)

    // Logging
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    implementation(projects.mazegameCommon)

    // Tests
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("MainKt")
}

tasks.withType<ShadowJar> {
    archiveBaseName.set(project.name)
    mergeServiceFiles()
}

tasks.test {
    useJUnitPlatform()
}
