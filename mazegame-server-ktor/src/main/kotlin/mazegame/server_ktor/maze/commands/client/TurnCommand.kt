package mazegame.server_ktor.maze.commands.client

import mazegame.server_ktor.maze.*

class TurnCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {
    private val rawDirection: String

    init {
        if (commandWithParameters.size != 2) {
            rawDirection = ""
            errorCode = ErrorCode.WRONG_PARAMETER_VALUE
        } else @Suppress("kotlin:S6518") // WTF? you serious?
        if (!clientConnection.isReady.get()) {
            rawDirection = ""
            errorCode = ErrorCode.ACTION_WITHOUT_READY
        } else {
            rawDirection = commandWithParameters[1]
        }
        checkLoggedIn()
    }

    override suspend fun internalExecute() {
        val movementAllowed = clientConnection.unready()
        if (!movementAllowed) {
            return
        }
        val messageToAll: Message
        val player: ServerPlayer = clientConnection.player
        when (rawDirection) {
            "r" -> {
                player.viewDirection = player.viewDirection.turnRight()
                messageToAll = createPlayerPositionTurnMessage(player)
            }

            "l" -> {
                player.viewDirection = player.viewDirection.turnLeft()
                messageToAll = createPlayerPositionTurnMessage(player)
            }

            else -> {
                errorCode = ErrorCode.WRONG_PARAMETER_VALUE
                return
            }
        }
        mazeServer.sendToAllPlayers(messageToAll)
    }
}
