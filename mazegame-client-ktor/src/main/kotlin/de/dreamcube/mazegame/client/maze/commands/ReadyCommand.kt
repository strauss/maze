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
 * This command indicates that the server is expecting the next move from the client.
 */
class ReadyCommand(mazeClient: MazeClient) : ClientSideCommand(mazeClient) {
    override val okay: Boolean = true

    /**
     * Makes the client determine the next move from the strategy and perform it.
     */
    override suspend fun internalExecute() {
        mazeClient.onReady()
    }
}