plugins {
    kotlin("jvm")
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

tasks.test {
    useJUnitPlatform()
}