package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class TermCommand(mazeClient: MazeClient) : ClientSideCommand(mazeClient) {
    override val okay: Boolean = true

    override suspend fun internalExecute() {
        // Indicates a connection termination from the server-side
        mazeClient.stop(false)
    }

}