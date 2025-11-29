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

package de.dreamcube.mazegame.client

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.events.ErrorInfoListener
import de.dreamcube.mazegame.common.maze.InfoCode
import kotlinx.coroutines.runBlocking

/**
 * Simple handler for dealing with duplicate nicknames. Uses the built-in retry mechanism for logging in.
 */
class DuplicateNickHandler(val mazeClient: MazeClient) : ErrorInfoListener {
    private var loginIteration = 0

    override fun onServerError(infoCode: InfoCode) {
        when (infoCode) {
            InfoCode.DUPLICATE_NICK -> {
                loginIteration += 1
                runBlocking { mazeClient.internalConnect(loginIteration) }
            }

            else -> {
                // ignore
            }
        }
    }
}