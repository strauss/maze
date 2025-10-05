package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.ErrorCode
import de.dreamcube.mazegame.server.maze.MazeServer


class ByeCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {

    init {
        if (commandWithParameters.size > 1) {
            errorCode = ErrorCode.PARAMETER_COUNT_INCORRECT
        }
        checkLoggedIn()
    }

    override suspend fun internalExecute() {
        clientConnection.stop()
    }
}