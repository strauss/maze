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

package de.dreamcube.mazegame.common.maze

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

/**
 * Generic command executor, based on coroutines. It is used by the client and the server.
 */
class CommandExecutor(private val parentScope: CoroutineScope) :
    CoroutineScope by CoroutineScope(parentScope.coroutineContext + SupervisorJob()) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CommandExecutor::class.java)
    }

    private val commandChannel = Channel<Command>(Channel.Factory.UNLIMITED)

    /**
     * This mutex ensures that only one command is executed at a time.
     */
    private val commandMutex = Mutex()

    fun start() = launch {
        LOGGER.info("Command executor started!")
        for (command in commandChannel) {
            commandMutex.withLock {
                try {
                    command.execute()
                } catch (ex: Exception) {
                    LOGGER.error("Error while executing command: ", ex)
                }
            }
        }
        LOGGER.info("Command executor stopped!")
    }

    /**
     * Adds a new command to the queue.
     */
    suspend fun addCommand(command: Command) = commandChannel.send(command)

}