plugins {
    kotlin("multiplatform")
    // optional: Kotlin-Serialization für gemeinsame DTOs
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    /* ------- Browser-Target (JS IR) ------------------------------ */
    js(IR) {
        browser {
            webpackTask {
                mainOutputFileName = "mazegame-web-ui.js"
                output.libraryTarget = "umd"
                output.library = "mazegameWebUi"
            }
        }
        binaries.executable()
    }

    /* ------- Gemeinsame SourceSets ------------------------------- */
    sourceSets {
        val commonMain by getting {
            dependencies {
                // JSON / Serialization (falls verwendet)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val jsMain by getting {
            dependencies {
                // kotlinx.html & DOM-Wrapper
                implementation(libs.kotlinx.html)
                implementation(libs.kotlinx.extensions)
            }
        }
    }
}

/* ------- Task: Bundle in den Server kopieren -------------------- */
tasks.register<Copy>("copyToServer") {
    group = LifecycleBasePlugin.BUILD_TASK_NAME
    description = "Copies the JS bundle into the server's static resources."

    // 1. Explizite Abhängigkeit:
    dependsOn("jsBrowserProductionWebpack")

//    from(layout.buildDirectory.dir("dist"))
    // 2. Quelle = genau das Output-Verzeichnis dieses Tasks
    from(
        tasks.named("jsBrowserProductionWebpack")
            .map { it.outputs.files }) { include("mazegame-web-ui.js") }

    into(
        project(":mazegame-server-ktor").layout.projectDirectory
            .dir("src/main/resources/static")
    )
}
project(":mazegame-server-ktor").tasks.named("processResources") {
    dependsOn(":mazegame-web-ui:copyToServer")
}