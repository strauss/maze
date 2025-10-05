package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.common.maze.ErrorCode
import de.dreamcube.mazegame.server.maze.*

class ChatCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {

    companion object {
        const val SERVER_MESSAGE_CODE = 200
        const val CLIENT_MESSAGE_CODE = 201
        const val CLIENT_WHISPER_CODE = 202
    }

    val infoCode: Int = if (commandWithParameters.size > 1) {
        commandWithParameters[1].toInt()
    } else {
        ErrorCode.PARAMETER_COUNT_INCORRECT.code
    }
    val message: String
    val targetId: Int

    init {
        if (commandWithParameters.size < 3) {
            errorCode = ErrorCode.PARAMETER_COUNT_INCORRECT
            message = ""
            targetId = -1
        } else {
            when (infoCode) {
                CLIENT_MESSAGE_CODE -> {
                    if (commandWithParameters.size > 3) {
                        errorCode = ErrorCode.PARAMETER_COUNT_INCORRECT
                        message = ""
                        targetId = -1
                    } else {
                        message = commandWithParameters[2]
                        targetId = -1
                    }
                }

                CLIENT_WHISPER_CODE -> {
                    if (commandWithParameters.size < 4) {
                        errorCode = ErrorCode.PARAMETER_COUNT_INCORRECT
                        message = ""
                        targetId = -1
                    } else {
                        message = commandWithParameters[2]
                        targetId = commandWithParameters[3].toInt()
                    }
                }

                else -> {
                    errorCode = ErrorCode.WRONG_PARAMETER_VALUE
                    message = ""
                    targetId = -1
                }
            }
        }
    }

    override suspend fun internalExecute() {
        if (clientConnection.chatControl.onSendMessage()) {
            if (infoCode == CLIENT_MESSAGE_CODE) {
                mazeServer.sendToAllPlayers(createClientInfoMessage(message, clientConnection.id))
            } else if (infoCode == CLIENT_WHISPER_CODE) {
                mazeServer.getClientConnection(targetId)?.sendMessage(createClientWhisperInfoMessage(message, clientConnection.id))
            }
        } else {
            clientConnection.sendMessage(ClientChatControl.FAILURE_MESSAGE)
        }
    }
}