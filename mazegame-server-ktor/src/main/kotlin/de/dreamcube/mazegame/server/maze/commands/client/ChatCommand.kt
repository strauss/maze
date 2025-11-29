/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.common.maze.MAX_CHAT_LENGTH
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
        if (message.length > MAX_CHAT_LENGTH) {
            clientConnection.chatControl.onPenalty()
            clientConnection.sendMessage(ClientChatControl.MESSAGE_TOO_LONG)
            return
        }
        if (clientConnection.chatControl.onSendMessage()) {
            if (infoCode == InfoCode.CLIENT_MESSAGE.code) {
                mazeServer.sendToAllPlayers(createClientInfoMessage(message, clientConnection.id))
            } else if (infoCode == InfoCode.CLIENT_WHISPER.code) {
                mazeServer.getClientConnection(targetId)
                    ?.sendMessage(createClientWhisperInfoMessage(message, clientConnection.id))
            }
        } else {
            clientConnection.sendMessage(ClientChatControl.FAILURE_MESSAGE)
        }
    }
}