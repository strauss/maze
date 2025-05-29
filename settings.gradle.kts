rootProject.name = "mazegame"

include("mazegame-server-ktor")   // weitere Module hier ergänzen

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()              // damit der alte Java-Client sofort gefunden wird
    }
}