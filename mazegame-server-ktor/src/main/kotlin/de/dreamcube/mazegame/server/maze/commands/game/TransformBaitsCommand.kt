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

import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand
import de.dreamcube.mazegame.server.maze.createEmptyLastMessage
import de.dreamcube.mazegame.server.maze.createServerInfoMessage
import kotlinx.coroutines.sync.withLock

class TransformBaitsCommand(mazeServer: MazeServer, val baitType: BaitType, val causingPlayerId: Int? = null) :
    ServerSideCommand(mazeServer) {
    override suspend fun execute() {
        if (baitType == BaitType.TRAP && !mazeServer.autoTrapeaterEnabled) {
            return
        }
        val messages: List<Message> = buildList {
            mazeServer.baitMutex.withLock {
                mazeServer.baitsById.values.forEach {
                    addAll(mazeServer.changeBait(it, baitType))
                }
            }
            if (isNotEmpty()) {
                val causingPlayerNick: String = mazeServer.getClientConnection(causingPlayerId)?.nick ?: "someone"
                when (baitType) {
                    BaitType.FOOD -> add(createServerInfoMessage("Oh no, $causingPlayerNick accidentally drank a cup of coffee from the office machine. All baits have been transformed to food.").thereIsMore())
                    BaitType.COFFEE -> add(createServerInfoMessage("Well ... $causingPlayerNick is so tired, they let all baits turn into the most delicious black coffee.").thereIsMore())
                    BaitType.GEM -> add(createServerInfoMessage("Yeah baby! It seems that $causingPlayerNick collected an enchanted golden apple. All baits have been transformed into gems.").thereIsMore())
                    BaitType.TRAP -> add(createServerInfoMessage("Oh no, $causingPlayerNick collected a blood diamond. All baits have turned into traps. But no worries, help has already arrived.").thereIsMore())
                }
                add(createEmptyLastMessage())
            }
        }
        if (messages.isNotEmpty()) {
            mazeServer.sendToAllPlayers(*messages.toTypedArray())
        }
        if (baitType == BaitType.TRAP) {
            mazeServer.autoTrapeaterHandler.spawn()
        } else {
            mazeServer.autoTrapeaterHandler.despawn()
        }
    }
}