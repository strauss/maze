package de.dreamcube.mazegame.ui

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.formdev.flatlaf.FlatLightLaf
import de.dreamcube.mazegame.client.DuplicateNickHandler
import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.MazeClient
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
import java.awt.EventQueue

class UiController {

    internal val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

    /**
     * The client, but only if the connection is established.
     */
    private lateinit var client: MazeClient

    private lateinit var clientTerminationHandle: Deferred<Unit>

    val connectionStatus: ConnectionStatus
        get() {
            return if (this::client.isInitialized) client.status else ConnectionStatus.UNKNOWN
        }

    internal fun connect(address: String, port: Int, strategyName: String, displayName: String) {
        val config = MazeClientConfigurationDto(address, port, strategyName, displayName)
        client = MazeClient(config)
        client.eventHandler.addEventListener(DuplicateNickHandler(client))
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