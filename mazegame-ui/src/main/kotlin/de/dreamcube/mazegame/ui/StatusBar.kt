package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.SpeedChangedListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.flavorText
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.KeyEvent
import javax.swing.*

class StatusBar() : JPanel(), ClientConnectionStatusListener, SpeedChangedListener {

    private val connectionStatusLabel = JLabel()
    private val serverAddressLabel = JLabel()
    private val strategyLabel = JLabel()
    private val botFlavorTextLabel = JLabel()
    private val gameSpeedLabel = JLabel()
    private val zoomLabel = JLabel()
    private val positionLabel = JLabel()
    private val serverControlButton = JButton("⎈")
    private val hintLabel = JLabel()

    init {
        UiController.statusBar = this
        val borderColor = UIManager.getColor("Separator.foreground")

        layout = BorderLayout()

        // Connection information
        val connectionPanel = JPanel()
        connectionPanel.layout = FlowLayout()
        connectionStatusLabel.text = UiController.connectionStatus.toString()
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

        gameStatusPanel.add(hintLabel)
        gameStatusPanel.add(positionLabel)
        gameStatusPanel.add(zoomLabel)
        gameStatusPanel.add(gameSpeedLabel)
        gameStatusPanel.add(serverControlButton)
        serverControlButton.isVisible = false
        serverControlButton.isEnabled = false
        serverControlButton.addActionListener { _ ->
            UiController.toggleServerControlView()
        }
        serverControlButton.mnemonic = KeyEvent.VK_H

        //3rd
        add(gameStatusPanel, BorderLayout.EAST)

        val topLine = BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor)
        border = topLine
        isOpaque = true
        UiController.prepareEventListener(this)
    }

    internal fun updateZoom(zoom: Int) {
        if (UiController.connectionStatus == ConnectionStatus.DEAD) {
            return
        }
        UiController.uiScope.launch {
            zoomLabel.text = "Zoom: $zoom"
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

    internal fun changeHintText(hintText: String) {
        hintLabel.text = hintText
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        UiController.uiScope.launch {
            connectionStatusLabel.text = newStatus.toString()
            serverAddressLabel.text = UiController.completeServerAddressString
            strategyLabel.text = UiController.strategyName?.let { "as $it (${UiController.getStrategyTypeForStatusBar()})" } ?: ""
            botFlavorTextLabel.text = UiController.strategyName?.flavorText() ?: ""
            zoomLabel.text = if (newStatus == ConnectionStatus.DEAD) "" else "Zoom: ${UiController.mazePanel.zoom}"
            gameSpeedLabel.text = if (newStatus == ConnectionStatus.DEAD) "" else "Speed: ${UiController.gameSpeed} ms"
            serverControlButton.isEnabled = UiController.isLoggedIn
        }
    }

    override fun onSpeedChanged(oldSpeed: Int, newSpeed: Int) {
        // TODO: as soon as the server is capable of speed changes, we should test this
        gameSpeedLabel.text = "Speed: ${UiController.gameSpeed} ms"
    }
}
