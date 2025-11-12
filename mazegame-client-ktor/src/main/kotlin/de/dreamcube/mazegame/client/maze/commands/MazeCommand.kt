package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

/**
 * This command indicates that the maze has been received.
 */
class MazeCommand(
    mazeClient: MazeClient,
    private val width: Int,
    private val height: Int,
    private val mazeLines: List<String>
) :
    ClientSideCommand(mazeClient) {
    override val okay: Boolean = true

    /**
     * Delegates the maze receiving directly to the client.
     */
    override suspend fun internalExecute() {
        mazeClient.initializeMaze(width, height, mazeLines)
    }
}