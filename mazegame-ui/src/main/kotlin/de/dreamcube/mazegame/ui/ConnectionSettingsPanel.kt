package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.isSpectatorStrategy
import de.dreamcube.mazegame.common.api.ReducedServerInformationDto
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
import javax.swing.*

class ConnectionSettingsPanel(private val controller: UiController) : JPanel(), ClientConnectionStatusListener {
    companion object {
        private const val TEXT_FIELD_COLUMNS: Int = 20
        private val LOGGER: Logger = LoggerFactory.getLogger(ConnectionSettingsPanel::class.java)

        private class ServerInfoPanel() : JPanel() {
            // Dummy panel for future minimap
            val dummyPanel = JPanel()
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
                layout = MigLayout("wrap 2")

                dummyPanel.preferredSize = Dimension(50, 50)
                add(dummyPanel, "west")

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

            fun activate(serverInfo: ReducedServerInformationDto) {
                idText.text = serverInfo.id.toString()
                dimensionsText.text = "${serverInfo.width} x ${serverInfo.height}"
                clientsText.text = "${serverInfo.activeClients} / ${serverInfo.maxClients}"
                speedText.text = "${serverInfo.speed} ms per tick"
                spectatorText.text = serverInfo.spectatorName ?: "<no spectators allowed>"
                isVisible = true
            }

            fun adjustLeftSide(preferredWidth: Int) {
                dummyPanel.preferredSize = Dimension(preferredWidth, dummyPanel.preferredSize.height)
            }

        }
    }

    private val managedConnection = JRadioButton("Managed")
    private val directConnection = JRadioButton("Direct")

    private val addressLabel = JLabel("Address")
    private val addressField = JTextField(TEXT_FIELD_COLUMNS)

    private val portLabel = JLabel("Port")
    private val portField = JTextField(TEXT_FIELD_COLUMNS)

    private val queryButton = JButton("Retrieve game information")

    private val gamePortLabel = JLabel("Game port")
    private val gamePortField = JTextField(TEXT_FIELD_COLUMNS)

    private val gameLabel = JLabel("Game")
    private val gameSelection = JComboBox<ReducedServerInformationDto?>()

    private val serverInfoPanel = ServerInfoPanel()

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

        // Connection type
        val connectionTypeGroup = ButtonGroup()
        connectionTypeGroup.add(managedConnection)
        connectionTypeGroup.add(directConnection)

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
        managedConnection.isSelected = true

        add(managedConnection)
        add(directConnection)

        // Address and port
        add(addressLabel)
        addressField.text = "localhost" // TODO: make it better
        add(addressField)

        add(portLabel)
        portField.text = "8080" // TODO: make it better
        add(portField)

        // Query button for managed connection
        add(JPanel()) // Dummy panel without content
        queryButton.preferredSize = Dimension(addressField.preferredSize.width, queryButton.preferredSize.height)
        queryButton.mnemonic = KeyEvent.VK_R
        add(queryButton)
        queryButton.addActionListener { _ ->
            val address = addressField.text
            val port = portField.text
            if (address.isNotBlank() && port.isNotBlank()) {
                queryButton.isEnabled = false
                controller.bgScope.launch {
                    try {
                        val gameInformationList: List<ReducedServerInformationDto> =
                            ServerCommandController.queryForGameInformation(address, port.toInt())
                        fillGameSelection(gameInformationList)
                        withContext(Dispatchers.Swing) {
                            gameSelection.isEnabled = true
                        }
                    } catch (ex: Exception) {
                        LOGGER.error("Error while retrieving game information from server: '${ex.message}'", ex)
                        withContext(Dispatchers.Swing) {
                            fillGameSelection(listOf())
                            JOptionPane.showMessageDialog(
                                this@ConnectionSettingsPanel,
                                "Error while retrieving game information from server.\nCheck address and port or use a direct connection.",
                                "Connection Error",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }
                    }
                    queryButton.isEnabled = true
                }
            }
        }

        add(gamePortLabel)
        add(gamePortField)

        add(gameLabel)
        add(gameSelection)
        gameSelection.isEnabled = false
        gameSelection.preferredSize = Dimension(addressField.preferredSize.width, gameSelection.preferredSize.height)
        fillGameSelection(listOf())
        gameSelection.addItemListener { event: ItemEvent ->
            if (event.stateChange == ItemEvent.SELECTED) {
                val item: ReducedServerInformationDto? = event.item as ReducedServerInformationDto
                if (item != null) {
                    gamePortField.text = item.id.toString()
                    addressField.isEnabled = false
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
                }
            } else if (event.stateChange == ItemEvent.DESELECTED) {
                gamePortField.text = ""
                addressField.isEnabled = true
                portField.isEnabled = true
                queryButton.isEnabled = true
                managedConnection.isEnabled = true
                directConnection.isEnabled = true
                serverControlPasswordField.isEnabled = false
                serverControlActivateButton.isEnabled = false
                serverInfoPanel.clear()
            }
        }

        add(serverInfoPanel, "span 2")
        serverInfoPanel.adjustLeftSide(addressLabel.preferredSize.width)
        add(JSeparator(), "span 2")

        // Server control stuff
        serverControlHeader.font = serverControlHeader.font.deriveFont(Font.BOLD)
        add(serverControlHeader, "span 2")

        serverControlPasswordField.echoChar = '★'
        serverControlPasswordField.isEnabled = false
        add(serverControlPasswordLabel)
        add(serverControlPasswordField)

        serverControlActivateButton.preferredSize = Dimension(addressField.preferredSize.width, serverControlActivateButton.preferredSize.height)
        serverControlActivateButton.isEnabled = false
        add(JPanel()) // Dummy panel without content
        add(serverControlActivateButton)
        serverControlActivateButton.addActionListener { _ ->
            serverControlActivateButton.isEnabled = false
            controller.bgScope.launch {
                try {
                    controller.activateServerController(
                        addressField.text,
                        portField.text.toInt(),
                        String(serverControlPasswordField.password),
                        gamePortField.text.toInt()
                    )
                    withContext(Dispatchers.Swing) {
                        gameSelection.isEnabled = false
                        serverControlPasswordField.text = "success"
                        serverControlPasswordField.isEnabled = false
                    }
                } catch (ex: ClientRequestException) {
                    LOGGER.error("Connection to server failed: ${ex.message}")
                    controller.deactivateServerController()
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
        strategySelection.preferredSize = Dimension(addressField.preferredSize.width, strategySelection.preferredSize.height)
        add(strategySelection)
        strategySelection.addItemListener { event: ItemEvent ->
            if (event.stateChange == ItemEvent.SELECTED) {
                val selectedStrategy: String = event.item as String
                val spectatorName: String? = (gameSelection.selectedItem as ReducedServerInformationDto?)?.spectatorName
                handleNick(selectedStrategy, spectatorName)
            }
        }

        add(nickLabel)
        add(nickField)

        add(JPanel())
        connectButton.preferredSize = Dimension(addressField.preferredSize.width, connectButton.preferredSize.height)
        add(connectButton)

        connectButton.addActionListener { _ ->
            val readyToConnect = controller.connectionStatus == ConnectionStatus.NOT_CONNECTED || controller.connectionStatus == ConnectionStatus.DEAD
            if (readyToConnect &&
                addressField.text.isNotBlank() &&
                gamePortField.text.isNotBlank() &&
                (strategySelection.selectedItem as String?)?.isNotBlank() ?: false &&
                nickField.text.isNotBlank()
            ) {
                controller.bgScope.launch {
                    controller.connect(
                        addressField.text,
                        gamePortField.text.toInt(),
                        strategySelection.selectedItem as String,
                        nickField.text
                    )
                }
                connectButton.isEnabled = false
            }
        }
        connectButton.mnemonic = KeyEvent.VK_C

        // TODO: here we could add an animated whatever to indicate, that the connection is being established

        add(JPanel(), "growy")

        controller.prepareEventListener(this)
    }

    private fun handleNick(selectedStrategy: String, spectatorName: String?) {
        controller.uiScope.launch {
            if (selectedStrategy.isSpectatorStrategy() && spectatorName != null) {
                nickField.text = spectatorName
                nickField.isEnabled = false
            } else {
                nickField.text = selectedStrategy
                nickField.isEnabled = true
            }
        }
    }

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
    }

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
        fillGameSelection(emptyList())

        // TODO: make these combo-boxes with predefined values, this should do for now
        addressField.text = "dreamcube.de"
        gamePortField.text = "12345"
    }

    private fun fillGameSelection(elements: List<ReducedServerInformationDto>) {
        val model: DefaultComboBoxModel<ReducedServerInformationDto?> = gameSelection.model as DefaultComboBoxModel
        if (model.size > 0) {
            model.removeAllElements()
        }
        model.addElement(null)
        model.addAll(elements)
    }

    private fun fillStrategySelection() {
        val availableStrategies: Set<String> = Strategy.getStrategyNamesBlocking()
        val model: DefaultComboBoxModel<String> = strategySelection.model as DefaultComboBoxModel
        if (model.size > 0) {
            model.removeAllElements()
        }
        model.addAll(availableStrategies)
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        controller.uiScope.launch {
            when (newStatus) {
                ConnectionStatus.CONNECTED -> this@ConnectionSettingsPanel.isEnabled = false
                ConnectionStatus.DEAD -> {
                    this@ConnectionSettingsPanel.isEnabled = true
                    connectButton.isEnabled = true
                    controller.deactivateServerController()
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