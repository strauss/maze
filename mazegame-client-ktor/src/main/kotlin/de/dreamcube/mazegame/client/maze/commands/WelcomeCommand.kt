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

/**
 * Indicates a successful login.
 */
class WelcomeCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    /**
     * The assigned id for the own player.
     */
    private val id: Int
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 2) {
            id = -1
            okay = false
        } else {
            id = commandWithParameters[1].toInt()
            okay = true
        }
    }

    /**
     * Delegates to the client, indicating a successful login. Also assigns the id to the client.
     */
    override suspend fun internalExecute() {
        mazeClient.loggedIn(id)
    }
}