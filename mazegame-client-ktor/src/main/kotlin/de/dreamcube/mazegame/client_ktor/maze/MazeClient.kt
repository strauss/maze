package de.dreamcube.mazegame.client_ktor.maze

import de.dreamcube.mazegame.client_ktor.config.MazeClientConfigurationDto
import io.ktor.network.sockets.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Central maze client class.
 */
class MazeClient(
    val clientConfiguration: MazeClientConfigurationDto,
    internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val NO_ID: Int = -1
        private val LOGGER: Logger = LoggerFactory.getLogger(MazeClient::class.java)
    }

    private val serverAddress
        get() = clientConfiguration.serverAddress

    private val serverPort
        get() = clientConfiguration.serverPort

    /**
     * The socket for the connection.
     */
    private lateinit var socket: Socket

    /**
     * The id from the server.
     */
    var id = NO_ID
        private set


}