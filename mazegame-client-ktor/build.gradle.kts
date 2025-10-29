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
    implementation(platform(project(":")))        // <dependencyManagement> der Basis

    // Ktor
//    implementation(libs.ktor.client.core)
//    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.network)
    implementation(libs.ktor.utils)

    // External dependencies
    implementation(libs.reflections)

    // Own stuff
    implementation(projects.mazegameCommon)

    // Tests
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("de.dreamcube.mazegame.client.HeadlessLauncherKt")
}

tasks.withType<ShadowJar> {
    archiveBaseName.set(project.name)
    mergeServiceFiles()
}

tasks.test {
    useJUnitPlatform()
}
