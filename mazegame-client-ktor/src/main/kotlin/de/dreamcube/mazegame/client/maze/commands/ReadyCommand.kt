package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class ReadyCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {
    override val okay: Boolean = true
    val gameSpeed: Int?

    init {
        gameSpeed = if (commandWithParameters.size > 1) {
            commandWithParameters[1].toInt()
        } else {
            null
        }
    }

    override suspend fun internalExecute() {
        if (gameSpeed != null) {
            mazeClient.gameSpeed = gameSpeed
        }
        mazeClient.onReady()
    }
}