package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class ReadyCommand(mazeClient: MazeClient) : ClientSideCommand(mazeClient) {
    override val okay: Boolean = true

    override suspend fun internalExecute() {
        mazeClient.onReady()
    }
}