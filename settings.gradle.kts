rootProject.name = "mazegame"

include("mazegame-common")
include("mazegame-client-ktor")
include("mazegame-server-ktor")
include("mazegame-ui")
// include("mazegame-web-ui") // it will become part of the game, but not just yet :-)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
