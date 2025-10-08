package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.server.maze.*

class ChatCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {

    val infoCode: Int = if (commandWithParameters.size > 1) {
        commandWithParameters[1].toInt()
    } else {
        InfoCode.PARAMETER_COUNT_INCORRECT.code
    }
    val message: String
    val targetId: Int

    init {
        if (commandWithParameters.size < 3) {
            errorCode = InfoCode.PARAMETER_COUNT_INCORRECT
            message = ""
            targetId = -1
        } else {
            when (infoCode) {
                InfoCode.CLIENT_MESSAGE.code -> {
                    if (commandWithParameters.size > 3) {
                        errorCode = InfoCode.PARAMETER_COUNT_INCORRECT
                        message = ""
                        targetId = -1
                    } else {
                        message = commandWithParameters[2]
                        targetId = -1
                    }
                }

                InfoCode.CLIENT_WHISPER.code -> {
                    if (commandWithParameters.size < 4) {
                        errorCode = InfoCode.PARAMETER_COUNT_INCORRECT
                        message = ""
                        targetId = -1
                    } else {
                        message = commandWithParameters[2]
                        targetId = commandWithParameters[3].toInt()
                    }
                }

                else -> {
                    errorCode = InfoCode.WRONG_PARAMETER_VALUE
                    message = ""
                    targetId = -1
                }
            }
        }
    }

    override suspend fun internalExecute() {
        if (clientConnection.chatControl.onSendMessage()) {
            if (infoCode == InfoCode.CLIENT_MESSAGE.code) {
                mazeServer.sendToAllPlayers(createClientInfoMessage(message, clientConnection.id))
            } else if (infoCode == InfoCode.CLIENT_WHISPER.code) {
                mazeServer.getClientConnection(targetId)?.sendMessage(createClientWhisperInfoMessage(message, clientConnection.id))
            }
        } else {
            clientConnection.sendMessage(ClientChatControl.FAILURE_MESSAGE)
        }
    }
}