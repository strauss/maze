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

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.PlayerSnapshot

/**
 * This command indicates that a player left the game.
 */
class LeaveCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    /**
     * The id of the player that is leaving.
     */
    private val playerId: Int
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 2) {
            playerId = -1
            okay = false
        } else {
            playerId = commandWithParameters[1].toInt()
            okay = true
        }
    }

    /**
     * Triggers a player logout event.
     */
    override suspend fun internalExecute() {
        val removedPlayer: PlayerSnapshot? = mazeClient.players.removePlayerById(playerId)
        removedPlayer?.let { player -> mazeClient.eventHandler.firePlayerLogout(player) }
    }
}