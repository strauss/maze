package de.dreamcube.mazegame.ui

import com.formdev.flatlaf.FlatLightLaf
import de.dreamcube.mazegame.client.DuplicateNickHandler
import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.events.EventListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.flavorText
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.isHumanStrategy
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.isSpectatorStrategy
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.ui.UiController.connect
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.EventQueue
import java.util.*

object UiController {
    private val LOGGER: Logger = LoggerFactory.getLogger(UiController::class.java)

    val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val uiEventListeners: MutableList<EventListener> = LinkedList()
    internal lateinit var uiPlayerCollection: UiPlayerCollection

    val mazeModel = MazeModel()
    internal lateinit var mazePanel: MazePanel
    internal lateinit var glassPane: GlassPane
    internal lateinit var statusBar: StatusBar
    internal lateinit var mainFrame: MainFrame
    internal lateinit var messagePane: MessagePane
    internal lateinit var scoreTable: ScoreTable
    internal var completeServerAddressString: String = "-"
    internal var strategyName: String? = null
    internal var flavorText: String? = null

    internal val gameSpeed: Int
        get() = if (this::client.isInitialized) client.gameSpeed else -1

    internal val ownId: Int
        get() = if (this::client.isInitialized) client.ownPlayer.id else -1

    /**
     * The client, but only if the connection is established.
     */
    private lateinit var client: MazeClient

    private lateinit var clientTerminationHandle: Deferred<Unit>

    internal var serverController: ServerCommandController? = null

    private val playerSelectionListeners: MutableList<PlayerSelectionListener> = LinkedList()

    private val serverControllerActive: Boolean
        get() = serverController != null

    internal val isLoggedIn: Boolean
        get() = client.isLoggedIn

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
        clientTerminationHandle = client.start()
        messagePane.clear()
        sendClientChatMessage("Connection established")
        completeServerAddressString = "@$address:$port"
        this.strategyName = strategyName
        runBlocking {
            flavorText = strategyName.flavorText()
        }
    }

    internal fun disconnect() {
        bgScope.launch {
            client.logout()
            completeServerAddressString = ""
            strategyName = null
            sendClientChatMessage("Disconnected")
        }
    }

    internal suspend fun onExit() {
        if (this::client.isInitialized && client.isLoggedIn) {
            client.logout()
        }
    }

    internal fun reset() {
        mazePanel.reset()
        messagePane.reset()
        scoreTable.reset()
    }

    internal fun triggerMazeUpdate(x: Int, y: Int) {
        uiScope.launch {
            mazePanel.updatePosition(x, y)
            mazePanel.repaint()
        }
    }

    internal suspend fun getStrategyTypeForStatusBar(): String {
        return strategyName?.let {
            when {
                it.isHumanStrategy() -> "Human"
                it.isSpectatorStrategy() -> "Spectator"
                else -> "Bot"
            }
        } ?: ""
    }

    internal fun updateZoom(zoom: Int) {
        if (this::statusBar.isInitialized) {
            statusBar.updateZoom(zoom)
        }
    }

    internal fun updatePositionStatus(x: Int, y: Int) {
        if (x < 0 || y < 0 || x >= mazeModel.width || y >= mazeModel.height) {
            statusBar.invalidPositionStatus()
        } else {
            statusBar.updatePositionStatus(x, y)
        }
    }

    /**
     * Triggers an event for displaying a client chat message. Only works, after the client object is created. Won't work, if it is "DEAD".
     */
    fun sendClientChatMessage(message: String) {
        if (this::client.isInitialized) {
            client.eventHandler.fireClientInfo(message)
        }
    }

    internal fun deactivateServerController() {
        serverController?.cancel()
        serverController = null
        statusBar.onNoServerControl()
    }

    internal suspend fun activateServerController(serverAddress: String, serverPort: Int, serverPassword: String, gamePort: Int) {
        serverController = ServerCommandController(bgScope, serverAddress, serverPort, serverPassword, gamePort)
        serverController?.loginOrRefreshToken()
        statusBar.onServerControl()
    }

    internal fun toggleServerControlView() {
        if (!serverControllerActive) {
            return
        }
        mainFrame.showOrHideServerControlPanel()
    }

    internal fun hintOnStatusBar(hintText: String) {
        statusBar.changeHintText(hintText)
    }

    internal fun clearHintOnStatusBar() {
        statusBar.changeHintText("")
    }

    fun addPlayerSelectionListener(playerSelectionListener: PlayerSelectionListener) {
        playerSelectionListeners.add(playerSelectionListener)
    }

    fun removePlayerSelectionListener(playerSelectionListener: PlayerSelectionListener) {
        playerSelectionListeners.remove(playerSelectionListener)
    }

    fun firePlayerSelected(player: UiPlayerInformation) {
        for (playerSelectionListener in playerSelectionListeners) {
            playerSelectionListener.onPlayerSelected(player)
        }
    }

    fun firePlayerSelectionCleared() {
        for (playerSelectionListener in playerSelectionListeners) {
            playerSelectionListener.onPlayerSelectionCleared()
        }
    }

}

fun main() {
    Strategy.scanAndAddStrategiesBlocking()
    FlatLightLaf.setup()
    EventQueue.invokeLater { MainFrame() }
}