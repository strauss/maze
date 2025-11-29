/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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