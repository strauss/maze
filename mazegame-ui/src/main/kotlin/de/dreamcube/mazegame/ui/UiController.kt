package de.dreamcube.mazegame.ui

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.formdev.flatlaf.FlatLightLaf
import de.dreamcube.mazegame.client.DuplicateNickHandler
import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.events.EventListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.common.control.ReducedServerInformationDto
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.EventQueue
import java.util.*

class UiController {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(UiController::class.java)
    }

    internal val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    internal val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val uiEventListeners: MutableList<EventListener> = LinkedList()

    /**
     * The client, but only if the connection is established.
     */
    private lateinit var client: MazeClient

    private lateinit var clientTerminationHandle: Deferred<Unit>

    val connectionStatus: ConnectionStatus
        get() {
            return if (this::client.isInitialized) client.status else ConnectionStatus.NOT_CONNECTED
        }

    /**
     * Collects [EventListener]s to be added to the [MazeClient] after it is created but before it is started. They are added when [connect] is
     * called.
     */
    fun prepareEventListener(eventListener: EventListener) {
        if (connectionStatus == ConnectionStatus.NOT_CONNECTED) {
            uiEventListeners.add(eventListener)
        } else {
            LOGGER.error("The client has already been established: '$connectionStatus'. Event listeners should be added directly.")
        }
    }

    internal fun connect(
        address: String,
        port: Int,
        strategyName: String,
        displayName: String
    ) {
        val config = MazeClientConfigurationDto(address, port, strategyName, displayName)
        client = MazeClient(config)
        client.eventHandler.addEventListener(DuplicateNickHandler(client))
        uiEventListeners.forEach { client.eventHandler.addEventListener(it) }
        uiEventListeners.clear() // a drop in the bucket but hey ... why not? RAM used to be expensive.
        clientTerminationHandle = client.start()
    }

    internal suspend fun queryForGameInformation(address: String, port: Int): List<ReducedServerInformationDto> {
        val httpAddress = "http://$address:$port/server"
        val httpClient = HttpClient(CIO) {
            this.expectSuccess = true
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }

        httpClient.use {
            return it.get(httpAddress).body()
        }
    }

}

fun main() {
    Strategy.scanAndAddStrategiesBlocking()
    FlatLightLaf.setup()
    EventQueue.invokeLater { MainFrame(UiController()) }
}