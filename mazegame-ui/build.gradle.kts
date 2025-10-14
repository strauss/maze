plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.formdev.flatlaf)
    implementation(libs.miglayout)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}