package mazegame.server_ktor.maze.commands.control

import de.dreamcube.mazegame.common.maze.Message
import mazegame.server_ktor.maze.MazeServer
import mazegame.server_ktor.maze.commands.ServerSideCommand

/**
 * Command for resetting all scores.
 */
class ClearCommand(mazeServer: MazeServer) : ServerSideCommand(mazeServer) {

    override suspend fun execute() {
        val messages: List<Message> = mazeServer.resetAllScores()
        if (messages.isNotEmpty()) {
            mazeServer.sendToAllPlayers(*messages.toTypedArray())
        }
    }
}