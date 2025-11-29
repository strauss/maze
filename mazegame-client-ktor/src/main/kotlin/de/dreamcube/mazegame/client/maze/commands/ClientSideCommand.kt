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
import de.dreamcube.mazegame.common.maze.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Abstract super class for all client-side commands.
 */
abstract class ClientSideCommand(protected val mazeClient: MazeClient) : Command {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ClientSideCommand::class.java)
    }

    /**
     * Indicator, if the command was correctly formatted and is ready to be processed.
     */
    protected abstract val okay: Boolean

    /**
     * Executes this command. Only does so, if [okay] is true. Ignores the command and logs an error otherwise.
     */
    override suspend fun execute() {
        if (okay) {
            internalExecute()
        } else {
            LOGGER.error("Server sent illegal '${javaClass.simpleName}' ... ignoring!")
        }
    }

    /**
     * The actual execution logic.
     */
    protected abstract suspend fun internalExecute()
}