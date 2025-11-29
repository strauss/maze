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

package de.dreamcube.mazegame.server.maze.commands.control

import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand

/**
 * Command for interrupting the game by withdrawing all baits. If [now] is set, all baits are withdrawn immediately. If not, only the traps are
 * withdrawn and the bots have to collect them for the final stop, but no new baits will be generated.
 */
class StopCommand(mazeServer: MazeServer, val now: Boolean = false) : ServerSideCommand(mazeServer) {

    override suspend fun execute() {
        mazeServer.desiredBaitCount.set(0)
        val messages: List<Message> =
            if (now) mazeServer.withdrawBaits() else mazeServer.withdrawBaits { it == BaitType.TRAP }
        if (messages.isNotEmpty()) {
            mazeServer.sendToAllPlayers(*messages.toTypedArray())
        }
        mazeServer.autoTrapeaterHandler.despawn()
    }
}