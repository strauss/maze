package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * If this command is received if the server confirmed a logout request from the client.
 */
class QuitCommand(mazeClient: MazeClient) : ClientSideCommand(mazeClient) {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(QuitCommand::class.java)
    }

    override val okay: Boolean = true

    /**
     * Just logs the confirmation, nothing more happens.
     */
    override suspend fun internalExecute() {
        // Indicates a connection termination from the client-side (response to BYE! command)
        LOGGER.info("Server confirmed logout.")
    }

}