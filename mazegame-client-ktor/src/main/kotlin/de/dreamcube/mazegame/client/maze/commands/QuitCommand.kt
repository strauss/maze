package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class QuitCommand(mazeClient: MazeClient) : ClientSideCommand(mazeClient) {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(QuitCommand::class.java)
    }

    override val okay: Boolean = true

    override suspend fun internalExecute() {
        // Indicates a connection termination from the client-side (response to BYE! command)
        LOGGER.info("Server confirmed logout.")
    }

}