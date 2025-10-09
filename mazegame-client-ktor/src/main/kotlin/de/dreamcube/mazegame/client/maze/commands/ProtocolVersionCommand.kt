package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

class ProtocolVersionCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {
    private val protocolVersion: Int
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 2) {
            protocolVersion = -1
            okay = false
        } else {
            protocolVersion = commandWithParameters[1].toInt()
            okay = true
        }
    }

    override suspend fun internalExecute() {
        mazeClient.connect(protocolVersion)
    }
}