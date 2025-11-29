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
import de.dreamcube.mazegame.common.maze.Command
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.createServerInfoMessage
import kotlinx.coroutines.CompletableDeferred

class PutBaitCommand(
    val mazeServer: MazeServer,
    val type: BaitType,
    val x: Int,
    val y: Int,
    val visible: Boolean = true,
    val reappearOffset: Long = 0L,
    val optionalMessage: String? = null
) :
    Command {
    val reply: CompletableDeferred<OccupationResult> = CompletableDeferred()

    override suspend fun execute() {
        try {
            val (result: OccupationResult, message: Message?) = mazeServer.putBait(type, x, y, visible, reappearOffset)
            if (result == OccupationResult.SUCCESS && message != null) {
                // When creating invisible baits, no message should be returned, despite the fact that it was successful
                if (optionalMessage == null) {
                    mazeServer.sendToAllPlayers(message)
                } else {
                    mazeServer.sendToAllPlayers(message.thereIsMore(), createServerInfoMessage(optionalMessage))
                }
            }
            reply.complete(result)
        } catch (e: Throwable) {
            reply.completeExceptionally(e)
            throw e
        }
    }
}