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

plugins {
    kotlin("jvm")
    `java-library`
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
