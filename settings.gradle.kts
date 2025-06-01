rootProject.name = "mazegame"

include("mazegame-server-ktor")
include("mazegame-web-ui")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}
