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
 * This command starts the handshake process between server and client. It is initiated by the server after the
 * connection has been established.
 */
class ProtocolVersionCommand(mazeClient: MazeClient, commandWithParameters: List<String>) :
    ClientSideCommand(mazeClient) {

    /**
     * The protocol version (should still be 1).
     */
    private val protocolVersion: Int

    override val okay: Boolean

    init {
        if (commandWithParameters.size < 2) {
            protocolVersion = -1
            okay = false
        } else {
            protocolVersion = commandWithParameters[1].toInt()
            okay = true
        }
    }

    /**
     * Makes the client to perform the next step on the road to a successful connection.
     */
    override suspend fun internalExecute() {
        mazeClient.connect(protocolVersion)
    }
}