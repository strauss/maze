/*
 * Maze Game
 * Copyright (c) 2026 Sascha Strauß
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

import de.dreamcube.mazegame.common.maze.*
import de.dreamcube.mazegame.server.maze.*

class BackStepCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {

    init {
        if (commandWithParameters.size != 1) {
            errorCode = InfoCode.WRONG_PARAMETER_VALUE
        } else {
            if (!clientConnection.isReady.get()) {
                errorCode = InfoCode.ACTION_WITHOUT_READY
            }
        }
        checkLoggedIn()
    }

    override suspend fun internalExecute() {
        val movementAllowed = clientConnection.unready()
        if (!movementAllowed) {
            return
        }
        val player: Player = clientConnection.player

        if (!mazeServer.maze.isWalkable(player.x, player.y, player.viewDirection.backwards())) {
            errorCode = InfoCode.WALL_CRASH
            return
        }

        val messagesForAll: MutableList<Message> = ArrayList()

        val currentPosition = PlayerPosition(player.x, player.y, player.viewDirection)
        val nextPosition = currentPosition.whenBackStep()
        val (nX, nY, dir) = nextPosition

        suspend fun move() {
            mazeServer.changePlayerPosition(player, nX, nY, dir)
            player.incrementMoveCounter()
            messagesForAll.add(createPlayerPositionMoveMessage(player).thereIsMore())
        }

        if (mazeServer.maze.isOccupied(nX, nY)) {
            // Backward movement is only allowed when the field "seems" free ... so invisible baits are okay, but
            // everything else is not
            val bait: ServerBait? = mazeServer.getBaitAt(nX, nY)
            if (bait == null || bait.visibleToClients) {
                // no bait -> must be a player, so we respond with a "wall crash"
                // visible bait -> wall crash
                errorCode = InfoCode.WALL_CRASH
                return
            }

            // At this point we have an invisible bait
            // An invisible gem (or coffee/food) becomes visible ... but we can't collect it
            if (bait.type != BaitType.TRAP) {
                bait.uncover()
                errorCode = InfoCode.WALL_CRASH
                // we don't return right away, because we need the replaceBait call below and sending all messages in the end
            } else {
                // we collect the bait and teleport the player
                val removeMessage: Message? = mazeServer.removeBait(bait)
                assert(removeMessage == null) // invisible baits don't yield a remove message
                player.score += bait.type.score // decrease ... this is always a trap
                messagesForAll.add(createPlayerScoreChangedMessage(player).thereIsMore())
                messagesForAll.add(createServerInfoMessage("${player.nick} 'collected' an invisible trap with their rear end ... what a mess!"))
                messagesForAll.add(mazeServer.teleportPlayerRandomly(player).thereIsMore())
                // we also give a penalty
                mazeServer.getClientConnection(player.id)?.additionalTickPenalty?.incrementAndGet()
            }

            // replace consumed bait ... required for uncovering and collecting
            val newBaitMessages: List<Message> = mazeServer.replaceBaits()
            if (newBaitMessages.isNotEmpty()) {
                messagesForAll.addAll(newBaitMessages)
            }
        } else {
            move()
        }

        if (messagesForAll.isNotEmpty()) {
            messagesForAll.add(createEmptyLastMessage())
        }
        mazeServer.sendToAllPlayers(*messagesForAll.toTypedArray())
    }
}