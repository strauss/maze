package de.dreamcube.mazegame.server.maze.server_bots

import de.dreamcube.mazegame.server.maze.ErrorCode
import mazegame.client.MazeClient
import mazegame.client.strategy.Strategy
import mazegame.client.strategy.SurpriseBot

class LegacyClientWrapper(val legacyClient: MazeClient) : ServerSideClient {

    override val clientId: Int
        get() = legacyClient.clientId

    override fun terminate() {
        legacyClient.sayGoodBye()
    }

    override val specialModeActive: Boolean
        get() {
            val strategy: Strategy? = legacyClient.strategy
            if (strategy is SurpriseBot) {
                return strategy.surpriseModeActive()
            }
            return false
        }

    override val loginFailed: Boolean
        get() = LOGIN_FAILED_CODES.contains(legacyClient.lastErrorCode)

    companion object {

        private val LOGIN_FAILED_CODES: Set<Int> =
            setOf(ErrorCode.LOGIN_TIMEOUT.code, ErrorCode.TOO_MANY_CLIENTS.code, ErrorCode.UNKNOWN_COMMAND.code, 999)

        init {
            Strategy.addStrategies()
        }

        /**
         * Creates, starts, and wraps a headless legacy client with the given [aliasName], that automatically connects to localhost at the given
         * [port]. If the client is not running, cannot be created, or is unable to connect for any reason, null is returned instead.
         */
        fun createServerSideClient(aliasName: String, port: Int): LegacyClientWrapper? {
            val mazeClient: MazeClient? = MazeClient.create(null, aliasName, "localhost", port)
            if (mazeClient?.isAlive ?: false) {
                return LegacyClientWrapper(mazeClient)
            }
            return null
        }

        /**
         * Delivers all available bot names for legacy clients in the classpath.
         */
        fun determineAvailableLegacyBotNames(): Set<String> {
            return Strategy.getStrategyNames()
        }

    }
}