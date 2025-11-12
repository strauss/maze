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