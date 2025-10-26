package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.SpeedChangedListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.flavorText
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class StatusBar(private val controller: UiController) : JPanel(), ClientConnectionStatusListener, SpeedChangedListener {

    private val connectionStatusLabel = JLabel()
    private val serverAddressLabel = JLabel()
    private val strategyLabel = JLabel()
    private val botFlavorTextLabel = JLabel()
    private val gameSpeedLabel = JLabel()
    private val zoomLabel = JLabel()
    private val positionLabel = JLabel()
    private val serverControlButton = JButton("⎈")

    init {
        controller.statusBar = this
        val borderColor = UIManager.getColor("Separator.foreground")

        layout = BorderLayout()

        // Connection information
        val connectionPanel = JPanel()
        connectionPanel.layout = FlowLayout()
        connectionStatusLabel.text = controller.connectionStatus.toString()
        serverAddressLabel.text = "-"
        strategyLabel.text = ""
        connectionPanel.add(connectionStatusLabel)
        connectionPanel.add(serverAddressLabel)
        connectionPanel.add(strategyLabel)
        connectionPanel.border = BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor)
        // 1st
        add(connectionPanel, BorderLayout.WEST)

        // Bot flavor text
        val botFlavorPanel = JPanel()
        botFlavorPanel.layout = FlowLayout()
        botFlavorTextLabel.text = ""
        botFlavorPanel.border = BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor)
        botFlavorPanel.add(botFlavorTextLabel)
        // 2nd
        add(botFlavorPanel, BorderLayout.CENTER)

        // Game status stuff
        val gameStatusPanel = JPanel()
        gameStatusPanel.layout = FlowLayout()

        invalidPositionStatus()

        gameStatusPanel.add(positionLabel)
        gameStatusPanel.add(zoomLabel)
        gameStatusPanel.add(gameSpeedLabel)
        gameStatusPanel.add(serverControlButton)
        serverControlButton.isVisible = false
        serverControlButton.isEnabled = false
        serverControlButton.addActionListener { _ ->
            controller.toggleServerControlView()
        }

        //3rd
        add(gameStatusPanel, BorderLayout.EAST)

        val topLine = BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor)
        border = topLine
        isOpaque = true
        controller.prepareEventListener(this)
    }

    internal fun updateZoom(zoom: Int) {
        if (controller.connectionStatus == ConnectionStatus.DEAD) {
            return
        }
        controller.uiScope.launch {
            zoomLabel.text = "Z: $zoom"
        }
    }

    internal fun updatePositionStatus(x: Int, y: Int) {
        positionLabel.text = "($x/$y)"
    }

    internal fun invalidPositionStatus() {
        positionLabel.text = "(-/-)"
    }

    internal fun onServerControl() {
        serverControlButton.isVisible = true
    }

    internal fun onNoServerControl() {
        serverControlButton.isVisible = false
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        controller.uiScope.launch {
            connectionStatusLabel.text = newStatus.toString()
            serverAddressLabel.text = controller.completeServerAddressString
            strategyLabel.text = controller.strategyName?.let { "as $it (${controller.getStrategyTypeForStatusBar()})" } ?: ""
            botFlavorTextLabel.text = controller.strategyName?.flavorText() ?: ""
            zoomLabel.text = if (newStatus == ConnectionStatus.DEAD) "" else "Zoom: ${controller.mazePanel.zoom}"
            gameSpeedLabel.text = if (newStatus == ConnectionStatus.DEAD) "" else "${controller.gameSpeed} ms"
            serverControlButton.isEnabled = controller.isLoggedIn
        }
    }

    override fun onSpeedChanged(oldSpeed: Int, newSpeed: Int) {
        // TODO: as soon as the server is capable of speed changes, we should test this
        gameSpeedLabel.text = "Speed: ${controller.gameSpeed}"
    }
}
