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

package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.common.maze.BaitPositionChange
import de.dreamcube.mazegame.common.maze.BaitType

/**
 * Command for receiving bait related events.
 */
class BaitPosCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    /**
     * [x] coordinate of the affected bait.
     */
    private val x: Int

    /**
     * [y] coordinate of the affected bait.
     */
    private val y: Int

    /**
     * [type] of the affected bait (see [BaitType])
     */
    private val type: BaitType

    /**
     * The [reason] for the change. It's either [BaitPositionChange.GENERATED] or [BaitPositionChange.COLLECTED].
     */
    private val reason: BaitPositionChange

    override val okay: Boolean

    init {
        if (commandWithParameters.size < 5) {
            x = -1
            y = -1
            type = BaitType.TRAP
            reason = BaitPositionChange.GENERATED
            okay = false
        } else {
            x = commandWithParameters[1].toInt()
            y = commandWithParameters[2].toInt()
            type = BaitType.byName(commandWithParameters[3])
            reason = BaitPositionChange.byName(commandWithParameters[4])
            okay = true
        }
    }

    /**
     * Adds or removes the bait and fires the corresponding event.
     */
    override suspend fun internalExecute() {
        when (reason) {
            BaitPositionChange.COLLECTED -> {
                mazeClient.baits.getBait(x, y)?.let { bait: Bait ->
                    val success: Boolean = mazeClient.baits.removeBait(bait)
                    if (success) {
                        mazeClient.eventHandler.fireBaitVanished(bait)
                    }
                }
            }

            BaitPositionChange.GENERATED -> {
                val bait = Bait(type, x, y)
                val success: Boolean = mazeClient.baits.addBait(bait)
                if (success) {
                    mazeClient.eventHandler.fireBaitAppeared(bait)
                }
            }
        }
    }
}