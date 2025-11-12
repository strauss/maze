package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient

/**
 * This command starts the handshake process between server and client. It is initiated by the server after the
 * connection has been established.
 */
class ProtocolVersionCommand(mazeClient: MazeClient, commandWithParameters: List<String>) :
    ClientSideCommand(mazeClient) {

    /**
     * The protocol version (should still be 1).
     */
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

    /**
     * Makes the client to perform the next step on the road to a successful connection.
     */
    override suspend fun internalExecute() {
        mazeClient.connect(protocolVersion)
    }
}