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

package de.dreamcube.mazegame.server.maze.commands.control

import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand


class KillCommand(mazeServer: MazeServer, val clientConnection: ClientConnection) : ServerSideCommand(mazeServer) {

    override suspend fun execute() {
        if (clientConnection.isAutoTrapeater) {
            mazeServer.autoTrapeaterHandler.despawn()
        } else if (clientConnection.isFrenzyBot) {
            mazeServer.frenzyHandler.despawn()
        }
        clientConnection.stop()
    }
}