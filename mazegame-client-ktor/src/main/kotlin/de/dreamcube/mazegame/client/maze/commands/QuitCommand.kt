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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * If this command is received if the server confirmed a logout request from the client.
 */
class QuitCommand(mazeClient: MazeClient) : ClientSideCommand(mazeClient) {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(QuitCommand::class.java)
    }

    override val okay: Boolean = true

    /**
     * Just logs the confirmation, nothing more happens.
     */
    override suspend fun internalExecute() {
        // Indicates a connection termination from the client-side (response to BYE! command)
        LOGGER.info("Server confirmed logout.")
    }

}