plugins {
    kotlin("jvm")
    alias(libs.plugins.jetbrains.dokka)
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation(libs.ktor.network)
    implementation(libs.ktor.utils)

    implementation(libs.jackson.module.kotlin)

    // Logging
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
