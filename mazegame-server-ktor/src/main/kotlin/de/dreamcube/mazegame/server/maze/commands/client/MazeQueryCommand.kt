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
import de.dreamcube.mazegame.server.maze.*

class MazeQueryCommand(
    clientConnection: ClientConnection,
    mazeServer: MazeServer,
    commandWithParameters: List<String>
) :
    ClientCommand(mazeServer, clientConnection) {

    init {
        if (commandWithParameters.size != 1) {
            errorCode = InfoCode.PARAMETER_COUNT_INCORRECT
        }
        checkLoggedIn()
    }

    override suspend fun internalExecute() {
        mazeServer.sendMapData(clientConnection)
        mazeServer.sendAllPlayerInfos(clientConnection)
        mazeServer.sendAllBaitInfos(clientConnection)
        clientConnection.sendMessage(createSpeedChangeInfoMessage(mazeServer.gameSpeed).thereIsMore())
        if (clientConnection.startAsSpectator) {
            startSpectating()
        } else {
            startPlaying()
        }
    }

    private suspend fun startSpectating() {
        clientConnection.spectate()
        sendWelcomeMessages()
        val players = mazeServer.activePlayers.get()
        if (players <= 0) {
            clientConnection.sendMessage(createServerInfoMessage("There is nobody to spectate!"))
        } else if (players == 1) {
            clientConnection.sendMessage(createServerInfoMessage("There is one payer playing."))
        } else {
            clientConnection.sendMessage(createServerInfoMessage("There are $players players playing."))
        }
    }

    private suspend fun startPlaying() {
        clientConnection.play()
        clientConnection.ready()
        sendWelcomeMessages()
        val otherPlayers = mazeServer.activePlayers.get() - 1
        if (otherPlayers <= 0) {
            clientConnection.sendMessage(createServerInfoMessage("You are alone ... for now!").thereIsMore())
        } else if (otherPlayers == 1) {
            clientConnection.sendMessage(createServerInfoMessage("One other player is already having fun.").thereIsMore())
        } else {
            clientConnection.sendMessage(createServerInfoMessage("$otherPlayers players are already having fun.").thereIsMore())
        }
        clientConnection.sendMessage(ClientChatControl.FIRST_CHAT_HINT)
    }

    private suspend fun sendWelcomeMessages() {
        clientConnection.sendMessage(createServerInfoMessage("Welcome to this amazing maze!").thereIsMore())
        clientConnection.sendMessage(createServerInfoMessage("The dimensions are ${mazeServer.maze.width} x ${mazeServer.maze.height}.").thereIsMore())
        clientConnection.sendMessage(createServerInfoMessage("The number of walkable fields is ${mazeServer.positionProvider.walkablePositionsSize}.").thereIsMore())
    }
}