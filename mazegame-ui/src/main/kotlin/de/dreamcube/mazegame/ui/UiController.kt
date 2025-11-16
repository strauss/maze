package de.dreamcube.mazegame.ui

import com.formdev.flatlaf.FlatLightLaf
import de.dreamcube.mazegame.client.DuplicateNickHandler
import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.ErrorInfoListener
import de.dreamcube.mazegame.client.maze.events.EventListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.flavorText
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.isHumanStrategy
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.isSpectatorStrategy
import de.dreamcube.mazegame.client.maze.strategy.VisualizationComponent
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.ui.UiController.connect
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.EventQueue
import java.util.*
import javax.swing.JOptionPane

object UiController {
    private val LOGGER: Logger = LoggerFactory.getLogger(UiController::class.java)

    val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val uiEventListeners: MutableList<EventListener> = LinkedList()
    internal lateinit var uiPlayerCollection: UiPlayerCollection

    val mazeModel = MazeModel()
    internal lateinit var mazePanel: MazePanel
    internal lateinit var markerPane: MarkerPane
    internal lateinit var statusBar: StatusBar
    internal lateinit var mainFrame: MainFrame
    internal lateinit var messagePane: MessagePane
    internal lateinit var scoreTable: ScoreTable
    internal var completeServerAddressString: String = "-"
    internal var strategyName: String? = null
    internal var flavorText: String? = null

    internal val gameSpeed: Int
        get() = if (this::client.isInitialized) client.gameSpeed else -1

    internal var ownId: Int = -1

    /**
     * The client, but only if the connection is established.
     */
    internal lateinit var client: MazeClient

    private lateinit var clientTerminationHandle: Deferred<Unit>

    internal var serverController: ServerCommandController? = null

    private val playerSelectionListeners: MutableList<PlayerSelectionListener> = LinkedList()

    private val mazeCellListeners: MutableList<MazeCellListener> = LinkedList()

    internal var visualizationComponent: VisualizationComponent? = null

    internal val serverControllerActive: Boolean
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

    private fun removeEventListener(eventListener: EventListener) {
        uiEventListeners.remove(eventListener)
    }

    internal fun connect(
        address: String,
        port: Int,
        strategyName: String,
        withFlavor: Boolean,
        displayName: String
    ) {
        val config = MazeClientConfigurationDto(address, port, strategyName, withFlavor, displayName)
        client = MazeClient(config)
        client.eventHandler.addEventListener(DuplicateNickHandler(client))

        val loginProblemListener: EventListener = object : ErrorInfoListener, ClientConnectionStatusListener {
            override fun onServerError(infoCode: InfoCode) {
                when (infoCode) {
                    InfoCode.WRONG_PARAMETER_VALUE -> {
                        showErrorMessage("Nickname not allowed!")
                        disconnect()
                    }

                    InfoCode.PARAMETER_COUNT_INCORRECT -> {
                        showErrorMessage("Protocol error: wrong parameter count.")
                        disconnect()
                    }

                    InfoCode.TOO_MANY_CLIENTS -> {
                        showErrorMessage("Server is full!")
                        disconnect()
                    }

                    InfoCode.LOGIN_TIMEOUT -> {
                        showErrorMessage("Connection timed out!")
                        disconnect()
                    }

                    else -> {
                        // ignore
                    }
                }
            }

            private fun showErrorMessage(message: String) {
                JOptionPane.showMessageDialog(
                    mainFrame,
                    message,
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }

            override fun onConnectionStatusChange(
                oldStatus: ConnectionStatus,
                newStatus: ConnectionStatus
            ) {
                val eventListener = this
                bgScope.launch {
                    delay(1_000L)
                    if (newStatus == ConnectionStatus.LOGGED_IN || newStatus == ConnectionStatus.DEAD) {
                        removeEventListener(eventListener)
                    }
                }
            }

        }

        prepareEventListener(loginProblemListener)
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

    internal fun updateOffset(x: Int, y: Int) {
        visualizationComponent?.updateOffset(x, y)
    }

    internal fun updateZoom(zoom: Int) {
        if (this::statusBar.isInitialized) {
            statusBar.updateZoom(zoom)
        }
        visualizationComponent?.zoom = zoom
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
    }

    internal suspend fun activateServerController(
        serverAddress: String,
        serverPort: Int,
        serverPassword: String,
        gamePort: Int
    ) {
        serverController = ServerCommandController(bgScope, serverAddress, serverPort, serverPassword, gamePort)
        serverController?.loginOrRefreshToken()
        mainFrame.initServerControlPanel()
        activateControlButton()
    }

    internal fun activateHoverMarks() {
        if (markerPane !in mazeCellListeners) {
            addMazeCellListener(markerPane)
        }
    }

    internal fun deactivateHoverMarks() {
        removeMazeCellListener(markerPane)
    }

    internal fun startPlaying() {
        ownId = client.ownPlayer.id
    }

    internal fun colorDistributionChanged(colorDistributionMap: Map<Int, Color>) {
        visualizationComponent?.colorDistributionMap = colorDistributionMap
    }

    internal fun activateControlButton() {
        statusBar.activateControlButton()
    }

    internal fun deactivateControlButton() {
        statusBar.deactivateControlButton()
    }

    internal fun toggleServerControlView() {
        mainFrame.showOrHideControlPanel()
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

    internal fun addMazeCellListener(mazeCellListener: MazeCellListener) {
        mazeCellListeners.add(mazeCellListener)
    }

    fun removePlayerSelectionListener(playerSelectionListener: PlayerSelectionListener) {
        playerSelectionListeners.remove(playerSelectionListener)
    }

    internal fun removeMazeCellListener(mazeCellListener: MazeCellListener) {
        mazeCellListeners.remove(mazeCellListener)
    }

    fun firePlayerSelected(player: UiPlayerInformation) {
        visualizationComponent?.selectedPlayerId = player.id
        for (playerSelectionListener in playerSelectionListeners) {
            playerSelectionListener.onPlayerSelected(player)
        }
    }

    fun firePlayerSelectionCleared() {
        visualizationComponent?.selectedPlayerId = null
        for (playerSelectionListener in playerSelectionListeners) {
            playerSelectionListener.onPlayerSelectionCleared()
        }
    }

    internal fun fireMazeCellSelected(x: Int, y: Int, mazeField: MazeModel.MazeField) {
        for (mazeCellSelectionListener in mazeCellListeners) {
            mazeCellSelectionListener.onMazeCellSelected(x, y, mazeField)
        }
    }

    internal fun fireMazeCellHovered(x: Int, y: Int, mazeField: MazeModel.MazeField) {
        for (mazeCellSelectionListener in mazeCellListeners) {
            mazeCellSelectionListener.onMazeCellHovered(x, y, mazeField)
        }
    }

}

fun main() {
    Strategy.scanAndAddStrategiesBlocking()
    FlatLightLaf.setup()
    EventQueue.invokeLater { MainFrame() }
}