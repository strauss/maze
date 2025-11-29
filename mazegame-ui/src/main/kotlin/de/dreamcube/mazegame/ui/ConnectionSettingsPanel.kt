/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.isHumanStrategyBlocking
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.isSpectatorStrategy
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.isSpectatorStrategyBlocking
import de.dreamcube.mazegame.common.api.ReducedServerInformationDto
import de.dreamcube.mazegame.common.maze.CompactMaze
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import net.miginfocom.swing.MigLayout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.io.encoding.Base64

private const val LOCALHOST = "localhost"
private const val DREAMCUBE = "dreamcube.de"

private const val MAX_PORT_NUMBER = 65535

/**
 * This class resembles the connection settings, that are shown on the left side, when the application is started.
 */
class ConnectionSettingsPanel() : JPanel(), ClientConnectionStatusListener {
    companion object {
        private const val TEXT_FIELD_COLUMNS: Int = 20
        private val LOGGER: Logger = LoggerFactory.getLogger(ConnectionSettingsPanel::class.java)
    }

    /**
     * This inner class shows the game information of the selected server.
     */
    private class ServerInfoPanel(preferredPreviewWidth: Int) : JPanel() {
        // Dummy panel for future minimap
        private val previewLabel = JLabel("No preview")
        private val idLabel = JLabel("Port")
        private val idText = JLabel()
        private val dimensionsLabel = JLabel("Dimensions")
        private val dimensionsText = JLabel()
        private val clientsLabel = JLabel("Clients")
        private val clientsText = JLabel()
        private val speedLabel = JLabel("Game speed")
        private val speedText = JLabel()
        private val spectatorLabel = JLabel("Spectator prefix")
        private val spectatorText = JLabel()

        init {
            layout = MigLayout("wrap 3", "[][][]")

            previewLabel.preferredSize = Dimension(preferredPreviewWidth, 50)
            add(previewLabel, "span 1 5")

            // Now we add the information
            idLabel.font = idLabel.font.deriveFont(Font.BOLD)
            add(idLabel)
            add(idText)

            dimensionsLabel.font = dimensionsLabel.font.deriveFont(Font.BOLD)
            add(dimensionsLabel)
            add(dimensionsText)

            clientsLabel.font = clientsLabel.font.deriveFont(Font.BOLD)
            add(clientsLabel)
            add(clientsText)

            speedLabel.font = speedLabel.font.deriveFont(Font.BOLD)
            add(speedLabel)
            add(speedText)

            spectatorLabel.font = spectatorLabel.font.deriveFont(Font.BOLD)
            add(spectatorLabel)
            add(spectatorText)

            clear()
        }

        fun clear() {
            idText.text = null
            dimensionsText.text = null
            clientsText.text = null
            speedText.text = null
            spectatorText.text = null
            isVisible = false
        }

        /**
         * Fills the panel with the information of the selected server/game. Renders a preview image of the map and
         * copies all values into the matching fields.
         */
        fun activate(serverInfo: ReducedServerInformationDto) {
            val compactMaze = CompactMaze.import(Base64.decode(serverInfo.compactMaze))
            val previewImage = BufferedImage(compactMaze.width * 2, compactMaze.height * 2, BufferedImage.TYPE_INT_RGB)
            for (y in 0..<compactMaze.height) {
                for (x in 0..<compactMaze.width) {
                    val colorAsRBG: Int = when (compactMaze[x, y]) {
                        CompactMaze.FieldValue.PATH -> 0xffffff // white
                        CompactMaze.FieldValue.WALL -> 0x777777 // darkish gray
                        CompactMaze.FieldValue.OUTSIDE -> 0x000000 // black
                        CompactMaze.FieldValue.UNKNOWN -> 0xff0000 // red
                    }
                    val xx = x * 2
                    val yy = y * 2
                    previewImage.setRGB(xx, yy, colorAsRBG)
                    previewImage.setRGB(xx + 1, yy, colorAsRBG)
                    previewImage.setRGB(xx + 1, yy + 1, colorAsRBG)
                    previewImage.setRGB(xx, yy + 1, colorAsRBG)
                }
            }
            previewLabel.text = null
            previewLabel.icon = ImageIcon(previewImage)
            previewLabel.horizontalAlignment = SwingConstants.CENTER
            previewLabel.verticalAlignment = SwingConstants.CENTER
            previewLabel.preferredSize = Dimension(previewImage.width + 20, previewImage.height)
            previewLabel.revalidate()
            previewLabel.repaint()
            idText.text = serverInfo.id.toString()
            dimensionsText.text = "${serverInfo.width} x ${serverInfo.height}"
            clientsText.text = "${serverInfo.activeClients} / ${serverInfo.maxClients}"
            speedText.text = "${serverInfo.speed} ms per tick"
            spectatorText.text = serverInfo.spectatorName ?: "<no spectators allowed>"
            isVisible = true
        }

        fun adjustLeftSide(preferredWidth: Int) {
            previewLabel.preferredSize = Dimension(preferredWidth, previewLabel.preferredSize.height)
        }

    }

    private val managedConnection = JRadioButton("Managed")
    private val directConnection = JRadioButton("Direct")
    private var withFlavor: Boolean = true

    private val addressLabel = JLabel("Address")
    private val addressFieldComboBox = JComboBox<String>()

    private val portLabel = JLabel("Port")
    private val portField = JTextField(TEXT_FIELD_COLUMNS)

    private val queryButton = JButton("Retrieve game information")

    private val gamePortLabel = JLabel("Game port")
    private val gamePortField = JTextField(TEXT_FIELD_COLUMNS)

    private val gameLabel = JLabel("Game")
    private val gameSelection = JComboBox<ReducedServerInformationDto?>()

    private val serverInfoPanel = ServerInfoPanel(addressLabel.preferredSize.width)

    private val serverControlHeader = JLabel("Server Control (optional)")
    private val serverControlPasswordLabel = JLabel("Password")
    private val serverControlPasswordField = JPasswordField(TEXT_FIELD_COLUMNS)
    private val serverControlActivateButton = JButton("Activate")

    private val strategyLabel = JLabel("Select")
    private val strategySelection = JComboBox<String>()

    private val nickLabel = JLabel("Nick")
    private val nickField = JTextField(TEXT_FIELD_COLUMNS)

    private val connectButton = JButton("Connect")

    init {
        layout = MigLayout("wrap 2", "[][grow]", "[]")
        val connectionLabel = JLabel("Connection")
        connectionLabel.font = connectionLabel.font.deriveFont(Font.BOLD)
        add(connectionLabel, "span")

        // Connection type (managed or direct)
        ButtonGroup().apply {
            add(managedConnection)
            add(directConnection)
        }

        managedConnection.addChangeListener { _ ->
            if (managedConnection.isSelected) {
                managedConnectionSelected()
            }
        }

        directConnection.addChangeListener { _ ->
            if (directConnection.isSelected) {
                directConnectionSelected()
            }
        }
        // The default is a managed connection ... never stick yourself to the past :-)
        managedConnection.isSelected = true

        add(managedConnection)
        add(directConnection)

        // Address and port
        add(addressLabel)
        val model: DefaultComboBoxModel<String> = addressFieldComboBox.model as DefaultComboBoxModel
        if (model.size > 0) {
            model.removeAllElements()
        }
        model.addElement(LOCALHOST)
        model.addElement(DREAMCUBE)
        addressFieldComboBox.isEditable = true
        addressFieldComboBox.selectedItem = LOCALHOST

        addressFieldComboBox.preferredSize =
            Dimension(portField.preferredSize.width, addressFieldComboBox.preferredSize.height)
        add(addressFieldComboBox)

        add(portLabel)
        portField.text = "8080"
        add(portField)

        // Query button for managed connection
        add(JPanel()) // Dummy panel without content
        queryButton.preferredSize = Dimension(portField.preferredSize.width, queryButton.preferredSize.height)
        queryButton.mnemonic = KeyEvent.VK_R
        add(queryButton)
        // When clicking the button, the server is queried
        queryButton.addActionListener { _ ->
            val address: String = addressFieldComboBox.selectedItem as String
            val port = portField.text
            if (address.isNotBlank() && port.isNotBlank()) {
                queryButton.isEnabled = false
                UiController.bgScope.launch {
                    try {
                        val serverHttpPort = readPortNumber(port)
                        if (serverHttpPort > 0) {
                            val gameInformationList: List<ReducedServerInformationDto> =
                                ServerCommandController.queryForGameInformation(address, serverHttpPort)
                            withContext(Dispatchers.Swing) {
                                fillGameSelection(gameInformationList)
                                gameSelection.isEnabled = true
                                portField.isEnabled = false
                                addressFieldComboBox.isEnabled = false
                            }
                        }
                    } catch (ex: Exception) {
                        LOGGER.error("Error while retrieving game information from server: '${ex.message}'", ex)
                        withContext(Dispatchers.Swing) {
                            clearGameSelection()
                            JOptionPane.showMessageDialog(
                                this@ConnectionSettingsPanel,
                                "Error while retrieving game information from server.\nCheck address and port or use a direct connection.",
                                "Connection Error",
                                JOptionPane.ERROR_MESSAGE
                            )
                            queryButton.isEnabled = true
                        }
                    }
                }
            }
        }

        add(gamePortLabel)
        add(gamePortField)

        add(gameLabel)
        add(gameSelection)
        gameSelection.isEnabled = false
        gameSelection.preferredSize = Dimension(portField.preferredSize.width, gameSelection.preferredSize.height)
        // Selecting the game is a further step in the configuration process
        // Several UI elements are activated or deactivated depending on the selection
        gameSelection.addItemListener { event: ItemEvent ->
            if (event.stateChange == ItemEvent.SELECTED) {
                val item: ReducedServerInformationDto? = event.item as ReducedServerInformationDto
                if (item != null && item.id > 0) {
                    gamePortField.text = item.id.toString()
                    addressFieldComboBox.isEnabled = false
                    portField.isEnabled = false
                    queryButton.isEnabled = false
                    managedConnection.isEnabled = false
                    directConnection.isEnabled = false
                    serverControlPasswordField.isEnabled = true
                    serverControlActivateButton.isEnabled = true
                    serverInfoPanel.activate(item)
                    val selectedStrategyName: String? = strategySelection.selectedItem as String?
                    if (selectedStrategyName != null) {
                        handleNick(selectedStrategyName, item.spectatorName)
                    }
                } else {
                    gamePortField.text = ""
                    addressFieldComboBox.isEnabled = true
                    portField.isEnabled = true
                    queryButton.isEnabled = true
                    managedConnection.isEnabled = true
                    directConnection.isEnabled = true
                    serverControlPasswordField.isEnabled = false
                    serverControlActivateButton.isEnabled = false
                    clearGameSelection()
                    serverInfoPanel.clear()
                }
            }
        }

        add(serverInfoPanel, "span 2, growx")
        serverInfoPanel.adjustLeftSide(addressLabel.preferredSize.width)
        add(JSeparator(), "span 2")

        // Server control stuff
        serverControlHeader.font = serverControlHeader.font.deriveFont(Font.BOLD)
        add(serverControlHeader, "span 2")

        // "fancy star"
        serverControlPasswordField.echoChar = '★'
        serverControlPasswordField.isEnabled = false
        add(serverControlPasswordLabel)
        add(serverControlPasswordField)

        serverControlActivateButton.preferredSize =
            Dimension(portField.preferredSize.width, serverControlActivateButton.preferredSize.height)
        serverControlActivateButton.isEnabled = false
        add(JPanel()) // Dummy panel without content
        add(serverControlActivateButton)
        serverControlActivateButton.addActionListener { _ ->
            serverControlActivateButton.isEnabled = false
            UiController.bgScope.launch {
                try {
                    UiController.activateServerController(
                        addressFieldComboBox.selectedItem as String,
                        portField.text.toInt(),
                        String(serverControlPasswordField.password),
                        gamePortField.text.toInt()
                    )
                    withContext(Dispatchers.Swing) {
                        gameSelection.isEnabled = false
                        serverControlPasswordField.text = "..."
                        serverControlPasswordField.isEnabled = false
                    }
                } catch (ex: ClientRequestException) {
                    LOGGER.error("Connection to server failed: ${ex.message}")
                    UiController.deactivateServerController()
                    UiController.deactivateControlButton()
                    withContext(Dispatchers.Swing) {
                        serverControlActivateButton.isEnabled = true
                        JOptionPane.showMessageDialog(
                            this@ConnectionSettingsPanel,
                            "Error while activating the server control. Check the server password and try again.",
                            "Connection Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
        }
        serverControlActivateButton.mnemonic = KeyEvent.VK_A

        // Strategy stuff
        val strategyHeader = JLabel("Strategy")
        strategyHeader.font = strategyHeader.font.deriveFont(Font.BOLD)
        add(strategyHeader, "span 2")

        add(strategyLabel)
        fillStrategySelection()
        strategySelection.preferredSize =
            Dimension(portField.preferredSize.width, strategySelection.preferredSize.height)
        add(strategySelection)
        strategySelection.addItemListener { event: ItemEvent ->
            if (event.stateChange == ItemEvent.SELECTED) {
                val selectedStrategy: String = event.item as String
                val spectatorName: String? = (gameSelection.selectedItem as ReducedServerInformationDto?)?.spectatorName
                handleNick(selectedStrategy, spectatorName)
            }
        }

        strategySelection.selectedIndex = 0
        nickField.text = strategySelection.selectedItem as String?

        add(nickLabel)
        add(nickField)

        add(JPanel())
        connectButton.preferredSize = Dimension(portField.preferredSize.width, connectButton.preferredSize.height)
        add(connectButton)

        // Finally the connection button
        connectButton.addActionListener { _ ->
            val readyToConnect =
                UiController.connectionStatus == ConnectionStatus.NOT_CONNECTED || UiController.connectionStatus == ConnectionStatus.DEAD
            if (readyToConnect &&
                (addressFieldComboBox.selectedItem as String).isNotBlank() &&
                gamePortField.text.isNotBlank() &&
                (strategySelection.selectedItem as String?)?.isNotBlank() ?: false &&
                nickField.text.isNotBlank()
            ) {
                UiController.bgScope.launch {
                    val gamePortNumber = readPortNumber(gamePortField.text)
                    if (gamePortNumber in 1..MAX_PORT_NUMBER) {
                        UiController.connect(
                            addressFieldComboBox.selectedItem as String,
                            gamePortNumber,
                            strategySelection.selectedItem as String,
                            withFlavor,
                            nickField.text
                        )
                        withContext(Dispatchers.Swing) {
                            connectButton.isEnabled = false
                        }
                    }
                }
            }
        }
        connectButton.mnemonic = KeyEvent.VK_C

        // TODO: here we could add an animated whatever to indicate, that the connection is being established

        add(JPanel(), "growy")

        UiController.prepareEventListener(this)
    }

    /**
     * Parses the port number. Yields an error, if it is not valid (not a number or not in the port range).
     */
    private suspend fun readPortNumber(portNumberAsString: String): Int {
        try {
            val parsedNumber = portNumberAsString.toInt()
            if (parsedNumber !in 1..MAX_PORT_NUMBER) {
                JOptionPane.showMessageDialog(
                    this@ConnectionSettingsPanel,
                    "The port number has to be between 1 and $MAX_PORT_NUMBER.",
                    "Error while connecting to the server",
                    JOptionPane.ERROR_MESSAGE
                )
                return -1
            }
            return parsedNumber
        } catch (_: NumberFormatException) {
            withContext(Dispatchers.Swing) {
                JOptionPane.showMessageDialog(
                    this@ConnectionSettingsPanel,
                    "The port NUMBER has to be an actual number.",
                    "Error while connecting to the server",
                    JOptionPane.ERROR_MESSAGE
                )
            }
            return -1
        }
    }

    /**
     * For managed connections this function automatically sets the nickname to the spectator's nickname, if a
     * spectator strategy is selected.
     */
    private fun handleNick(selectedStrategy: String, spectatorName: String?) {
        UiController.uiScope.launch {
            if (selectedStrategy.isSpectatorStrategy() && spectatorName != null) {
                nickField.text = spectatorName
                nickField.isEnabled = false
            } else {
                nickField.text = selectedStrategy
                nickField.isEnabled = true
            }
        }
    }

    /**
     * Activates and deactivates UI elements for configuring a managed connection.
     */
    private fun managedConnectionSelected() {
        portLabel.isVisible = true
        portField.isVisible = true
        queryButton.isVisible = true
        gamePortLabel.isEnabled = false
        gamePortField.isEnabled = false
        gameLabel.isVisible = true
        gameSelection.isVisible = true
        serverControlHeader.isVisible = true
        serverControlPasswordLabel.isVisible = true
        serverControlPasswordField.isVisible = true
        serverControlActivateButton.isVisible = true
        withFlavor = true
    }

    /**
     * Activates and deactivates UI elements for configuring a direct connection (aka "old style").
     */
    private fun directConnectionSelected() {
        portLabel.isVisible = false
        portField.isVisible = false
        queryButton.isVisible = false
        gamePortLabel.isEnabled = true
        gamePortField.isEnabled = true
        gameLabel.isVisible = false
        gameSelection.isVisible = false
        serverControlHeader.isVisible = false
        serverControlPasswordLabel.isVisible = false
        serverControlPasswordField.isVisible = false
        serverControlActivateButton.isVisible = false
        withFlavor = false
        clearGameSelection()

        addressFieldComboBox.selectedItem = DREAMCUBE
        gamePortField.text = "12345"
    }

    /**
     * Fills the game selection combobox with the meta information received from the server.
     */
    private fun fillGameSelection(elements: List<ReducedServerInformationDto>) {
        val model: DefaultComboBoxModel<ReducedServerInformationDto?> = gameSelection.model as DefaultComboBoxModel
        if (model.size > 0) {
            model.removeAllElements()
        }
        val actualList = buildList {
            // The first one resembles the "Clear..." text for resetting the received game information.
            add(ReducedServerInformationDto(-1, -1, -1, -1, -1, -1, ""))
            addAll(elements)
        }
        model.addAll(actualList)
    }

    private fun clearGameSelection() {
        val model: DefaultComboBoxModel<ReducedServerInformationDto?> = gameSelection.model as DefaultComboBoxModel
        if (model.size > 0) {
            model.removeAllElements()
        }
    }

    private enum class StrategyType {
        BOT, SPECTATOR, HUMAN
    }

    /**
     * Retrieves the available strategies from [Strategy]'s companion object (or static area, if you prefer). They are
     * sorted by their strategy type (bots first, then spectators, then human strategies) and by their name
     * (case-insensitive).
     */
    private fun fillStrategySelection() {
        val availableStrategies: Set<String> = Strategy.getStrategyNamesBlocking()
        val classifiedStrategies: List<Pair<String, StrategyType>> = availableStrategies.map {
            val type = when {
                it.isSpectatorStrategyBlocking() -> StrategyType.SPECTATOR
                it.isHumanStrategyBlocking() -> StrategyType.HUMAN
                else -> StrategyType.BOT
            }
            Pair(it, type)
        }
        val compare = compareBy<Pair<String, StrategyType>> { it.second }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first }
            .thenBy { it.first }
        val sortedStrategies = classifiedStrategies.sortedWith(compare)

        val model: DefaultComboBoxModel<String> = strategySelection.model as DefaultComboBoxModel
        if (model.size > 0) {
            model.removeAllElements()
        }
        sortedStrategies.forEach {
            model.addElement(it.first)
        }
    }

    /**
     * React to connection events. As soon as we are connected, this part of the UI is disabled. When the client dies,
     * the UI is put into a state where it can set up a new connection.
     */
    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        UiController.uiScope.launch {
            when (newStatus) {
                ConnectionStatus.CONNECTED -> this@ConnectionSettingsPanel.isEnabled = false
                ConnectionStatus.DEAD -> {
                    this@ConnectionSettingsPanel.isEnabled = true
                    connectButton.isEnabled = true
                    UiController.deactivateServerController()
                    UiController.deactivateControlButton()
                    serverControlPasswordField.text = ""
                    serverControlPasswordField.isEnabled = true
                    serverControlActivateButton.isEnabled = true
                    gameSelection.isEnabled = true
                }

                else -> {
                    // nothing
                }
            }
        }
    }
}