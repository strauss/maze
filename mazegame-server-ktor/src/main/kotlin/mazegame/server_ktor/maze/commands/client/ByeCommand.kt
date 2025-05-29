package mazegame.server_ktor.maze.commands.client

import mazegame.server_ktor.maze.ClientConnection
import mazegame.server_ktor.maze.ErrorCode
import mazegame.server_ktor.maze.MazeServer

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