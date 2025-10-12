plugins {
    `java-platform`
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.dokka)
}

group = "de.dreamcube"
version = "1.0-SNAPSHOT"

javaPlatform { allowDependencies() }   // erlaubt echte Abhängigkeitsangaben

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

        // ---- optionale Test-Libs ----------
        api(libs.junit.jupiter)
        // api(libs.mockito.core)
    }
}
