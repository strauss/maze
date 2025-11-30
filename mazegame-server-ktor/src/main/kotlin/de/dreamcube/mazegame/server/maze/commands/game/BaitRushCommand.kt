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

package de.dreamcube.mazegame.server.maze.commands.game

import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand
import de.dreamcube.mazegame.server.maze.createEmptyLastMessage
import de.dreamcube.mazegame.server.maze.createServerInfoMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BaitRushCommand(
    mazeServer: MazeServer,
    val causingPlayerId: Int? = null,
    val duration: Long = mazeServer.serverConfiguration.game.events.eventCooldown / 2
) :
    ServerSideCommand(mazeServer) {
    override suspend fun execute() {
        val baseBaitCount: Int = mazeServer.baseBaitCount
        val desiredBaitCount: Int = mazeServer.desiredBaitCount.get()
        if (baseBaitCount == desiredBaitCount) {
            // set up the bait rush
            mazeServer.desiredBaitCount.set(baseBaitCount * 2)
            val messages: List<Message> = buildList {
                addAll(mazeServer.replaceBaits())
                val causingPlayerNick: String = mazeServer.getClientConnection(causingPlayerId)?.nick ?: "someone"
                add(createServerInfoMessage("Nice one! It seems $causingPlayerNick stepped on an invisible pressure plate and caused more baits to spawn ... at least temporarily."))
                add(createEmptyLastMessage())
            }
            mazeServer.sendToAllPlayers(*messages.toTypedArray())

            // prepare restore to normal
            mazeServer.scope.launch {
                delay(duration)
                val currentDesiredBaitCount = mazeServer.desiredBaitCount.get()
                // if we stopped, the desiredBaitCount will be 0. This condition will catch the 0 case and the "we are
                // already normal" case.
                if (currentDesiredBaitCount > baseBaitCount) {
                    mazeServer.desiredBaitCount.set(baseBaitCount)
                    mazeServer.sendToAllPlayers(createServerInfoMessage("Oh, it is over. The number of baits is normalizing."))
                }
            }
        }
    }
}