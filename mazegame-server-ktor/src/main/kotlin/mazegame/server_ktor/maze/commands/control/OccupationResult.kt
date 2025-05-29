package mazegame.server_ktor.maze.commands.control

enum class OccupationResult {
    SUCCESS, FAIL_OUT_OF_BOUNDS, FAIL_NO_PATH, FAIL_OCCUPIED
}