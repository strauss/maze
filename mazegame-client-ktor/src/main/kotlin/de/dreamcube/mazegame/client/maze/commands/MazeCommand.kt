package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class MazeCommand(mazeClient: MazeClient, private val width: Int, private val height: Int, private val mazeLines: List<String>) :
    ClientSideCommand(mazeClient) {
    override suspend fun execute() {
        mazeClient.initializeMaze(width, height, mazeLines)
    }
}