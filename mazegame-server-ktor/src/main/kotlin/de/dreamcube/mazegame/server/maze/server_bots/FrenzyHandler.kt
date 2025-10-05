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
        LOGGER.info("Frenzy bot '$botAlias' spawned.")
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