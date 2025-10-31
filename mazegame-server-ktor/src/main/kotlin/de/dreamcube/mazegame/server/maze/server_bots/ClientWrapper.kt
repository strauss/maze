package de.dreamcube.mazegame.server.maze.server_bots

import de.dreamcube.mazegame.client.DuplicateNickHandler
import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.events.ErrorInfoListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.client.maze.strategy.SurpriseBot
import de.dreamcube.mazegame.common.maze.InfoCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking

class ClientWrapper(val client: MazeClient) : ServerSideClient, ErrorInfoListener {

    val clientDeferred: Deferred<Unit>

    init {
        client.eventHandler.addEventListener(this)
        client.eventHandler.addEventListener(DuplicateNickHandler(client))
        clientDeferred = client.start()
    }

    override val clientId: Int
        get() = client.id

    override fun terminate() {
        runBlocking { client.logout() }
    }

    override val specialModeActive: Boolean
        get() {
            val strategy: Strategy = client.strategy
            if (strategy is SurpriseBot) {
                return strategy.surpriseModeActive()
            }
            return false
        }

    private var internalConnectionFailed = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override val connectionFailed: Boolean
        get() = internalConnectionFailed || (!clientDeferred.isActive && clientDeferred.getCompletionExceptionOrNull()
            ?.let { it !is CancellationException } ?: false)

    override fun onServerError(infoCode: InfoCode) {
        if (infoCode == InfoCode.TOO_MANY_CLIENTS || infoCode == InfoCode.LOGIN_TIMEOUT) {
            internalConnectionFailed = true
        }
    }

    companion object {
        init {
            Strategy.scanAndAddStrategiesBlocking()
        }

        /**
         * Creates and wraps and starts a client with the given [aliasName], that automatically connects to localhost at the given [port].
         */
        internal fun createServerSideClient(
            aliasName: String,
            port: Int,
            displayName: String = aliasName
        ): ClientWrapper {
            val config = MazeClientConfigurationDto("localhost", port, aliasName, displayName)
            val mazeClient = MazeClient(config)
            return ClientWrapper(mazeClient)
        }

        /**
         * Determines all available bot names in the classpath.
         */
        fun determineAvailableBotNames(): Set<String> {
            return Strategy.getStrategyNamesBlocking()
        }
    }
}