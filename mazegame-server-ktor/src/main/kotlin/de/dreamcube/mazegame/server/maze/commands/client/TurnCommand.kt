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
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.common.maze.Player
import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.createPlayerPositionTurnMessage

class TurnCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {
    private val rawDirection: String

    init {
        if (commandWithParameters.size != 2) {
            rawDirection = ""
            errorCode = InfoCode.WRONG_PARAMETER_VALUE
        } else @Suppress("kotlin:S6518") // WTF? you serious?
        if (!clientConnection.isReady.get()) {
            rawDirection = ""
            errorCode = InfoCode.ACTION_WITHOUT_READY
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
        val player: Player = clientConnection.player
        when (rawDirection) {
            "r" -> {
                player.viewDirection = player.viewDirection.turnRight()
                player.incrementMoveCounter()
                messageToAll = createPlayerPositionTurnMessage(player)
            }

            "l" -> {
                player.viewDirection = player.viewDirection.turnLeft()
                player.incrementMoveCounter()
                messageToAll = createPlayerPositionTurnMessage(player)
            }

            else -> {
                errorCode = InfoCode.WRONG_PARAMETER_VALUE
                return
            }
        }
        mazeServer.sendToAllPlayers(messageToAll)
    }
}
