package de.dreamcube.mazegame.server.maze.commands.control

import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand

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