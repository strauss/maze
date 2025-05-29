package mazegame.server_ktor.maze.commands

/**
 * Interface for executing commands.
 */
fun interface Command {
    suspend fun execute()
}