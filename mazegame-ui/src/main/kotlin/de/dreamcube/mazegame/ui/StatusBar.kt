package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.SpeedChangedListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.flavorText
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyEvent
import javax.swing.*

class StatusBar() : JPanel(), ClientConnectionStatusListener, SpeedChangedListener, PlayerSelectionListener {

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
        // 1st
        add(connectionPanel, BorderLayout.WEST)

        // Bot flavor text
        val botFlavorPanel = JPanel()
        botFlavorPanel.layout = FlowLayout()
        botFlavorTextLabel.text = ""
        botFlavorPanel.add(botFlavorTextLabel)
        // 2nd
        add(botFlavorPanel, BorderLayout.CENTER)

        // Game status stuff
        val gameStatusPanel = JPanel()
        gameStatusPanel.layout = FlowLayout(FlowLayout.RIGHT)
        gameStatusPanel.preferredSize = Dimension(300, gameStatusPanel.preferredSize.height)

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
        val dummyPanel = JPanel()
        dummyPanel.preferredSize = Dimension(2, dummyPanel.preferredSize.height)
        gameStatusPanel.add(dummyPanel)

        //3rd
        add(gameStatusPanel, BorderLayout.EAST)

        val topLine = BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor)
        border = topLine
        isOpaque = true
        UiController.prepareEventListener(this)
        UiController.addPlayerSelectionListener(this)
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

    internal fun activateControlButton() {
        serverControlButton.isVisible = true
    }

    internal fun deactivateControlButton() {
        serverControlButton.isVisible = false
    }

    internal fun changeHintText(hintText: String) {
        hintLabel.text = hintText
    }

    private fun changeFlavorText(flavorText: String?) {
        UiController.uiScope.launch {
            botFlavorTextLabel.text = flavorText ?: "This bot is too old!"
        }
    }

    private suspend fun resetFlavorText() {
        botFlavorTextLabel.text = UiController.strategyName?.flavorText() ?: ""
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        UiController.uiScope.launch {
            connectionStatusLabel.text = newStatus.toString()
            serverAddressLabel.text = UiController.completeServerAddressString
            strategyLabel.text =
                UiController.strategyName?.let { "as $it (${UiController.getStrategyTypeForStatusBar()})" } ?: ""
            resetFlavorText()
            zoomLabel.text = if (newStatus == ConnectionStatus.DEAD) "" else "Zoom: ${UiController.mazePanel.zoom}"
            gameSpeedLabel.text = if (newStatus == ConnectionStatus.DEAD) "" else "Speed: ${UiController.gameSpeed} ms"
            serverControlButton.isEnabled = UiController.isLoggedIn
        }
    }

    override fun onSpeedChanged(oldSpeed: Int, newSpeed: Int) {
        UiController.uiScope.launch {
            // TODO: as soon as the server is capable of speed changes, we should test this
            gameSpeedLabel.text = "Speed: ${UiController.gameSpeed} ms"
        }
    }

    override fun onPlayerSelected(player: UiPlayerInformation) {
        changeFlavorText(player.snapshot.flavor)
    }

    override fun onPlayerSelectionCleared() {
        UiController.uiScope.launch {
            resetFlavorText()
        }
    }
}
