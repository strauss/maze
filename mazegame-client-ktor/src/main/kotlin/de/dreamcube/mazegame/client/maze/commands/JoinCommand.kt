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
import de.dreamcube.mazegame.client.maze.view
import de.dreamcube.mazegame.common.maze.Player

/**
 * This command indicates, that a new player has joined the game (aka logged into the server).
 */
class JoinCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    /**
     * The ID of the new player.
     */
    private val playerId: Int

    /**
     * The nickname of the new player.
     */
    private val nick: String

    /**
     * The optional flavor text of the new player.
     */
    private val flavor: String?

    override val okay: Boolean

    init {
        if (commandWithParameters.size < 3) {
            playerId = -1
            nick = ""
            okay = false
        } else {
            playerId = commandWithParameters[1].toInt()
            nick = commandWithParameters[2]
            okay = true
        }
        flavor = if (commandWithParameters.size > 3) commandWithParameters[3] else null
    }

    /**
     * Fires a player login event. If it is the own player id, in addition, an own player login event is fired. This
     * also initializes the [MazeClient.ownPlayer] reference.
     */
    override suspend fun internalExecute() {
        val newPlayer = Player(playerId, nick, flavor)
        val success: Boolean = mazeClient.players.addPlayer(newPlayer)
        if (success) {
            val newPlayerView = newPlayer.view()
            val newPlayerSnapshot = newPlayerView.takeSnapshot()
            mazeClient.eventHandler.firePlayerLogin(newPlayerSnapshot)
            if (playerId == mazeClient.id) {
                mazeClient.ownPlayer = newPlayerView
                mazeClient.eventHandler.fireOwnPlayerLogin(newPlayerSnapshot)
            }
        }
    }
}