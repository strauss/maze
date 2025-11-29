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

import de.dreamcube.mazegame.server.maze.MazeServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FrenzyHandler(mazeServer: MazeServer) : ServerBotHandler(mazeServer) {
    override val botAlias: String
        get() = mazeServer.serverConfiguration.serverBots.specialBots.frenzy

    override suspend fun handle() {
        mazeServer.getClientConnection(client?.clientId)?.delayCompensator?.penaltyTime =
            computeFrenzyPenaltyTime(client?.specialModeActive ?: false)
    }

    /**
     * Attempts to spawn the frenzy bot manually.
     */
    suspend fun spawnManually(associateInBackground: Boolean = true) {
        spawn(associateInBackground)
    }

    override suspend fun postSpawn() {
        if (client != null) {
            LOGGER.info("Frenzy bot '$botAlias' spawned.")
        } else {
            LOGGER.warn("Frenzy bot '$botAlias' could not spawn.")
        }
    }

    override suspend fun postDespawn() {
        // nothing
    }

    private fun computeFrenzyPenaltyTime(specialMode: Boolean): Long {
        if (!specialMode) {
            return 0
        }
        val currentDelay = mazeServer.gameSpeed.delay
        // This ensures roughly double speed
        return -(currentDelay - (currentDelay / 2))
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(FrenzyHandler::class.java)
    }

}