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

package de.dreamcube.mazegame.server.maze.server_bots

/**
 * Allows for server side clients. Currently, only the legacy clients are possible, but this interface is "future-proof".
 */
interface ServerSideClient {

    /**
     * The id of the client to allow the server a correlation with player IDs
     */
    val clientId: Int

    /**
     * Allows the server to terminate the client from the server side.
     */
    fun terminate()

    /**
     * Allows the server to check if the bot has its "special mode" active. Currently, this can only be the frenzy mode of the frenzy bot. The special
     * mode allows the server to accelerate the bot if it is meant to "cheat".
     */
    val specialModeActive: Boolean

    /**
     * Indicates that the client could not connect.
     */
    val connectionFailed: Boolean
}