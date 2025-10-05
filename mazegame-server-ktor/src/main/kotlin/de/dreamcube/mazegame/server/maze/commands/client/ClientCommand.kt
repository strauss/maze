package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.common.maze.COMMAND_AND_MESSAGE_SEPARATOR
import de.dreamcube.mazegame.common.maze.ErrorCode
import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand
import de.dreamcube.mazegame.server.maze.createInfoMessage


/**
 * A command represents a received message from a client. The preconditions are checked in the constructor (init...) and the errorCode is set
 * accordingly. The [execute] functions checks the error code and only continues, if the [errorCode] is [de.dreamcube.mazegame.common.maze.ErrorCode.OK]. Every command has to implement
 * the [internalExecute] function, which is called by [execute].
 */
abstract class ClientCommand(mazeServer: MazeServer, val clientConnection: ClientConnection) : ServerSideCommand(mazeServer) {

    var errorCode = ErrorCode.OK
        protected set

    val okay
        get() = errorCode == ErrorCode.OK

    protected fun checkLoggedIn() {
        if (!clientConnection.loggedIn()) {
            errorCode = ErrorCode.COMMAND_BEFORE_LOGIN
        }
    }

    override suspend fun execute() {
        if (okay) {
            internalExecute()
            // If something went wrong during execution, we send an error message according to the error code
            if (!okay) {
                clientConnection.sendMessage(createInfoMessage(errorCode))
            }
        } else {
            clientConnection.sendMessage(createInfoMessage(errorCode))
        }
    }

    /**
     * Does the actual work of the command. The conditional checks are covered by the error codes.
     */
    protected abstract suspend fun internalExecute()
}

class UnknownCommand(clientConnection: ClientConnection, mazeServer: MazeServer) : ClientCommand(mazeServer, clientConnection) {
    init {
        errorCode = ErrorCode.UNKNOWN_COMMAND
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