package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class WelcomeCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {
    val id: Int

    init {
        if (commandWithParameters.size < 2) {
            TODO("ERROR")
        }
        id = commandWithParameters[1].toInt()
    }

    override suspend fun execute() {
        mazeClient.loggedIn(id)
    }
}