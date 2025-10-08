package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class LeaveCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    private val playerId: Int

    init {
        if (commandWithParameters.size < 2) {
            TODO("ERROR")
        }
        playerId = commandWithParameters[1].toInt()
    }

    override suspend fun execute() {
        val success: Boolean = mazeClient.players.removePlayerById(playerId)
        if (success) {
            // TODO: send event
        }
    }
}