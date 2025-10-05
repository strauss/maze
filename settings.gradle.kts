rootProject.name = "mazegame"

include("mazegame-common")
include("mazegame-client-ktor")
include("mazegame-server-ktor")
include("mazegame-web-ui")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
