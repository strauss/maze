package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class WelcomeCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {
    private val id: Int
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 2) {
            id = -1
            okay = false
        } else {
            id = commandWithParameters[1].toInt()
            okay = true
        }
    }

    override suspend fun internalExecute() {
        mazeClient.loggedIn(id)
    }
}