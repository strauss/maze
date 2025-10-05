package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class MazeCommand(mazeClient: MazeClient, width: Int, height: Int, mazeLines: List<String>) : ClientSideCommand(mazeClient) {
    override suspend fun execute() {
        TODO("Not yet implemented")
    }
}