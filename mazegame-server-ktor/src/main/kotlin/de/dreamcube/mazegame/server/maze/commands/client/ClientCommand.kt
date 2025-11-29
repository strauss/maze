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

package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.common.maze.COMMAND_AND_MESSAGE_SEPARATOR
import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand
import de.dreamcube.mazegame.server.maze.createErrorInfoMessage


/**
 * A command represents a received message from a client. The preconditions are checked in the constructor (init...) and the errorCode is set
 * accordingly. The [execute] functions checks the error code and only continues, if the [errorCode] is [de.dreamcube.mazegame.common.maze.InfoCode.OK]. Every command has to implement
 * the [internalExecute] function, which is called by [execute].
 */
abstract class ClientCommand(mazeServer: MazeServer, val clientConnection: ClientConnection) :
    ServerSideCommand(mazeServer) {

    var errorCode = InfoCode.OK
        protected set

    val okay
        get() = errorCode == InfoCode.OK

    protected fun checkLoggedIn() {
        if (!clientConnection.loggedIn()) {
            errorCode = InfoCode.COMMAND_BEFORE_LOGIN
        }
    }

    override suspend fun execute() {
        if (okay) {
            internalExecute()
            // If something went wrong during execution, we send an error message according to the error code
            if (!okay) {
                clientConnection.sendMessage(createErrorInfoMessage(errorCode))
            }
        } else {
            clientConnection.sendMessage(createErrorInfoMessage(errorCode))
        }
    }

    /**
     * Does the actual work of the command. The conditional checks are covered by the error codes.
     */
    protected abstract suspend fun internalExecute()
}

class UnknownCommand(clientConnection: ClientConnection, mazeServer: MazeServer) :
    ClientCommand(mazeServer, clientConnection) {
    init {
        errorCode = InfoCode.UNKNOWN_COMMAND
    }

    override suspend fun internalExecute() {
        // do nothing
    }
}

fun createCommand(clientConnection: ClientConnection, mazeServer: MazeServer, rawCommand: String): ClientCommand {
    val commandWithParameters: List<String> = rawCommand.split(COMMAND_AND_MESSAGE_SEPARATOR)
    if (commandWithParameters.isEmpty()) {
        return UnknownCommand(clientConnection, mazeServer)
    }
    val command: String = commandWithParameters[0]
    return try {
        when (command) {
            "STEP" -> StepCommand(clientConnection, mazeServer, commandWithParameters)
            "TURN" -> TurnCommand(clientConnection, mazeServer, commandWithParameters)
            "INFO" -> ChatCommand(clientConnection, mazeServer, commandWithParameters)
            "HELO" -> HelloCommand(clientConnection, mazeServer, commandWithParameters)
            "MAZ?" -> MazeQueryCommand(clientConnection, mazeServer, commandWithParameters)
            "BYE!" -> ByeCommand(clientConnection, mazeServer, commandWithParameters)
            else -> UnknownCommand(clientConnection, mazeServer)
        }
    } catch (_: Exception) {
        UnknownCommand(clientConnection, mazeServer)
    }
}