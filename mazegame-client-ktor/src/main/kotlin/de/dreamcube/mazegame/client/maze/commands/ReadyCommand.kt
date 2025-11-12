package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

/**
 * This command indicates that the server is expecting the next move from the client.
 */
class ReadyCommand(mazeClient: MazeClient) : ClientSideCommand(mazeClient) {
    override val okay: Boolean = true

    /**
     * Makes the client determine the next move from the strategy and perform it.
     */
    override suspend fun internalExecute() {
        mazeClient.onReady()
    }
}