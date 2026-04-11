/*
 * Maze Game
 * Copyright (c) 2025-2026 Sascha Strauß
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
import de.dreamcube.mazegame.common.maze.sanitizeAsChatMessage
import de.dreamcube.mazegame.server.maze.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChatCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ChatCommand::class.java)
    }

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
                        message = commandWithParameters[2].sanitizeAsChatMessage()
                        targetId = -1
                    }
                }

                InfoCode.CLIENT_WHISPER.code -> {
                    if (commandWithParameters.size < 4) {
                        errorCode = InfoCode.PARAMETER_COUNT_INCORRECT
                        message = ""
                        targetId = -1
                    } else {
                        message = commandWithParameters[2].sanitizeAsChatMessage()
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
                LOGGER.info("${clientConnection.nick}(${clientConnection.id}): $message")
                mazeServer.sendToAllPlayers(createClientInfoMessage(message, clientConnection.id))
            } else if (infoCode == InfoCode.CLIENT_WHISPER.code) {
                val target = mazeServer.getClientConnection(targetId)
                if (target != null) {
                    LOGGER.info("${clientConnection.nick}(${clientConnection.id}) -> ${target.nick}(${target.id}): $message")
                    target.sendMessage(createClientWhisperInfoMessage(message, clientConnection.id))
                } else {
                    LOGGER.warn("${clientConnection.nick}(${clientConnection.id}) -> $targetId (FAILED): $message")
                }
            }
        } else {
            clientConnection.sendMessage(ClientChatControl.FAILURE_MESSAGE)
        }
    }
}