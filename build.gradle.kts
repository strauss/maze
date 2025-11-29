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
    `java-platform`
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.dokka)
    `maven-publish`
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        the<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>().jvmToolchain(21)
    }
}

javaPlatform { allowDependencies() }   // erlaubt echte Abhängigkeitsangaben

publishing {
    publications {
        create<MavenPublication>("bom") {
            from(components["javaPlatform"])
            groupId = "de.dreamcube.maze"
            artifactId = "maze-bom"
        }
    }
}

dependencies {
    /* -------- BOM-Importe direkt als API-Abhängigkeiten -------- */
    api(platform("org.jetbrains.kotlin:kotlin-bom:${libs.versions.kotlin.get()}"))
    api(platform("io.ktor:ktor-bom:${libs.versions.ktor.get()}"))
    api(platform("com.fasterxml.jackson:jackson-bom:${libs.versions.jackson.get()}"))

    /* -------- Einzel-Constraints (keine platform()-Notation nötig) -------- */
    constraints {
        api(libs.slf4j.api)
        api(libs.logback.classic)
        api(libs.jwt)
        api("org.jetbrains.kotlin:kotlin-stdlib")
        api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        // ---- optionale Test-Libs ----------
        api(libs.junit.jupiter)
        // api(libs.mockito.core)
    }
}
