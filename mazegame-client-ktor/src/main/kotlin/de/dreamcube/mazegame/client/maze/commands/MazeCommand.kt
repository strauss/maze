package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class MazeCommand(mazeClient: MazeClient, private val width: Int, private val height: Int, private val mazeLines: List<String>) :
    ClientSideCommand(mazeClient) {
    override val okay: Boolean = true

    override suspend fun internalExecute() {
        mazeClient.initializeMaze(width, height, mazeLines)
    }
}