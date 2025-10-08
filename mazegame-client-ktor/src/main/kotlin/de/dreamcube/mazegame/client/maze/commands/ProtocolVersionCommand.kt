package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class ProtocolVersionCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {
    private val protocolVersion: Int

    init {
        if (commandWithParameters.size < 2) {
            TODO("ERROR")
        }
        protocolVersion = commandWithParameters[1].toInt()
    }

    override suspend fun execute() {
        mazeClient.connect(protocolVersion)
    }
}