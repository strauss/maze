package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

/**
 * This command is received, if the server tells all clients that it is terminating.
 */
class TermCommand(mazeClient: MazeClient) : ClientSideCommand(mazeClient) {
    override val okay: Boolean = true

    /**
     * The client is told to stop playing.
     */
    override suspend fun internalExecute() {
        // Indicates a connection termination from the server-side
        mazeClient.stop(false)
    }

}