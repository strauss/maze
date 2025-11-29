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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Abstraction of server-side bots that can only be spawned once, such as the trapeater or the frenzy bot.
 */
abstract class ServerBotHandler(protected val mazeServer: MazeServer) {

    /**
     * Server-side client including the bot.
     */
    internal var client: ServerSideClient? = null

    /**
     * Tells, if the bot is running.
     */
    val active: Boolean
        get() = client != null

    abstract val botAlias: String

    protected val mutex: Mutex = Mutex()

    /**
     * Checks, if the bot should spawn or despawn and should do so. Can also perform other adjustments associated with it.
     */
    abstract suspend fun handle()

    /**
     * Spawns the bot if it is not already spawned.
     */
    internal suspend fun spawn(associateInBackground: Boolean = true) {
        mutex.withLock {
            if (client == null) {
                client = mazeServer.internalSpawnServerSideBot(botAlias)
                if (associateInBackground) {
                    mazeServer.associateBotWithClientConnectionInTheBackground(client)
                } else {
                    mazeServer.associateBotWithClientConnection(client)
                }
                postSpawn()
            }
        }
    }

    /**
     * Enables decisions after the bot has spawned.
     */
    protected abstract suspend fun postSpawn()

    /**
     * Despawns the bot if it is spawned.
     */
    internal suspend fun despawn() {
        mutex.withLock {
            client?.terminate()
            client = null
        }
        postDespawn()
    }

    /**
     * Enables decisions after the bot has despawned
     */
    protected abstract suspend fun postDespawn()

}