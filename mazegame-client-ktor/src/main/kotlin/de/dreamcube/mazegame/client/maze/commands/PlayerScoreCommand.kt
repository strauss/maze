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
 * This command indicates a score change.
 */
class PlayerScoreCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    /**
     * The id of the affected player.
     */
    private val playerId: Int

    /**
     * The new [score] of the affected player.
     */
    private val score: Int

    override val okay: Boolean

    init {
        if (commandWithParameters.size < 3) {
            playerId = -1
            score = -1
            okay = false
        } else {
            playerId = commandWithParameters[1].toInt()
            score = commandWithParameters[2].toInt()
            okay = true
        }
    }

    /**
     * Triggers a score change event.
     */
    override suspend fun internalExecute() {
        mazeClient.players.changePlayerScore(playerId, score)
            ?.let { (oldScore, playerSnapshot): Pair<Int, PlayerSnapshot> ->
                mazeClient.eventHandler.fireScoreChange(oldScore, playerSnapshot)
            }
    }

}