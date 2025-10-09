package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.common.maze.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class ClientSideCommand(protected val mazeClient: MazeClient) : Command {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ClientSideCommand::class.java)
    }

    protected abstract val okay: Boolean

    override suspend fun execute() {
        if (okay) {
            internalExecute()
        } else {
            LOGGER.error("Server sent illegal '${javaClass.simpleName}' ... ignoring!")
        }
    }

    protected abstract suspend fun internalExecute()
}