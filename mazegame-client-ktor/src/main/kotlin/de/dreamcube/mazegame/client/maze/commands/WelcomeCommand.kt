package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

/**
 * Indicates a successful login.
 */
class WelcomeCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    /**
     * The assigned id for the own player.
     */
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

    /**
     * Delegates to the client, indicating a successful login. Also assigns the id to the client.
     */
    override suspend fun internalExecute() {
        mazeClient.loggedIn(id)
    }
}