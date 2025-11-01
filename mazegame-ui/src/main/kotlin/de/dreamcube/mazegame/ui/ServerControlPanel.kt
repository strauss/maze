package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.common.api.PlayerInformationDto
import de.dreamcube.mazegame.common.api.ServerInformationDto
import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.util.Disposer
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import net.miginfocom.swing.MigLayout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Font
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import javax.swing.*

private const val SG_UNITY = "sg unity"
private const val SPAN_2 = "span 2"

class ServerControlPanel() : JPanel() {

    companion object {
        private const val NO_TEXT = "-"
        private val LOGGER: Logger = LoggerFactory.getLogger(ServerControlPanel::class.java)

        private class DisposeOnEscape(val disposer: Disposer) : KeyEventDispatcher {
            override fun dispatchKeyEvent(e: KeyEvent?): Boolean {
                if (e?.id == KeyEvent.KEY_PRESSED && e.keyCode == KeyEvent.VK_ESCAPE) {
                    disposer.close()
                    return true
                }
                return false
            }
        }
    }

    private val serverController: ServerCommandController
        get() = UiController.serverController ?: throw IllegalStateException("The server controller vanished.")

    private val selectableNicks = JComboBox<String>()
    private val spawnButton = JButton("Spawn")
    lateinit var availableServersideNicks: List<String>

    init {
        layout = MigLayout("insets 5, wrap 2", "[grow,fill][grow,fill]")
        initGameControlElements()
        initBaitControlElements()
        initPlayerControlElements()
        // TODO: contest control

//        val borderColor = UIManager.getColor("Separator.foreground")
//        val fancyBorder = BorderFactory.createMatteBorder(1, 1, 0, 0, borderColor)
//        border = fancyBorder
        isOpaque = true
        spawnButton.isEnabled = false
        serverController.launch {
            try {
                val serverInformation: ServerInformationDto = serverController.serverInformation()
                availableServersideNicks = serverInformation.gameInformation.availableBotNames
                withContext(Dispatchers.Swing) {
                    val model: DefaultComboBoxModel<String> = selectableNicks.model as DefaultComboBoxModel
                    if (model.size > 0) {
                        model.removeAllElements()
                    }
                    if (availableServersideNicks.isEmpty()) {
                        spawnButton.isEnabled = false
                    } else {
                        model.addAll(availableServersideNicks)
                        selectableNicks.selectedIndex = 0
                        spawnButton.isEnabled = true
                    }
                }
            } catch (ex: ResponseException) {
                withContext(Dispatchers.Swing) {
                    showErrorMessage(ex)
                    spawnButton.isEnabled = false
                }
            }
        }
    }

    private fun initGameControlElements() {
        // Header
        val gameHeader = JLabel("Game control")
        gameHeader.font = gameHeader.font.deriveFont(Font.BOLD)
        add(gameHeader, SPAN_2)

        // Go command
        val goButton = JButton("Go")
        goButton.mnemonic = KeyEvent.VK_G
        goButton.addActionListener { _ ->
            goButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.go()
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    goButton.isEnabled = true
                }
            }
        }
        add(goButton, SG_UNITY)

        // Clear command
        val clearButton = JButton("Clear")
        clearButton.mnemonic = KeyEvent.VK_C
        clearButton.addActionListener { _ ->
            clearButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.clear()
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    clearButton.isEnabled = true
                }
            }
        }
        add(clearButton, SG_UNITY)

        // Stop command
        val stopButton = JButton("Stop")
        stopButton.mnemonic = KeyEvent.VK_S
        stopButton.addActionListener { _ ->
            stopButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.stop()
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    stopButton.isEnabled = true
                }
            }
        }
        add(stopButton, SG_UNITY)

        // Stop now command
        val stopNowButton = JButton("Stop now")
        stopNowButton.mnemonic = KeyEvent.VK_N
        stopNowButton.addActionListener { _ ->
            stopNowButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.stop(true)
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    stopNowButton.isEnabled = true
                }
            }
        }
        add(stopNowButton, SG_UNITY)
    }

    private fun initBaitControlElements() {
        // Header
        val baitHeader = JLabel("Bait control")
        baitHeader.font = baitHeader.font.deriveFont(Font.BOLD)
        add(baitHeader, SPAN_2)

        // bait selection
        val baitTypeSelection = JComboBox<BaitType>()
        val model: DefaultComboBoxModel<BaitType?> = baitTypeSelection.model as DefaultComboBoxModel
        if (model.size > 0) {
            model.removeAllElements()
        }
        model.addAll(BaitType.entries)
        baitTypeSelection.selectedItem = BaitType.COFFEE
        baitTypeSelection.isEditable = false
        add(baitTypeSelection, SPAN_2)

        // Transform
        val baitTransformButton = JButton("Transform")
        baitTransformButton.addActionListener { _ ->
            val baitType: BaitType = baitTypeSelection.selectedItem as BaitType
            baitTransformButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.baitTransform(baitType)
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    baitTransformButton.isEnabled = true
                }
            }
        }

        // Put bait
        val putBaitButton = object : JButton("Put"), PlayerSelectionListener {
            override fun onPlayerSelected(player: UiPlayerInformation) {
                isEnabled = false
            }

            override fun onPlayerSelectionCleared() {
                isEnabled = true
            }
        }
        UiController.addPlayerSelectionListener(putBaitButton)
        putBaitButton.mnemonic = KeyEvent.VK_P
        putBaitButton.addActionListener { _ ->
            putBaitButton.isEnabled = false
            UiController.hintOnStatusBar("Select position")

            val disposer = Disposer()
            disposer.addDisposeAction {
                putBaitButton.isEnabled = true
                UiController.clearHintOnStatusBar()
            }

            val mazeCellSelectionListener = MazeCellSelectionListener { x, y ->
                val baitType: BaitType = baitTypeSelection.selectedItem as BaitType
                serverController.launch {
                    try {
                        serverController.baitPut(baitType, x, y)
                    } catch (ex: ResponseException) {
                        withContext(Dispatchers.Swing) {
                            showErrorMessage(ex)
                        }
                    }
                    withContext(Dispatchers.Swing) {
                        disposer.close()
                    }
                }
            }
            UiController.mazePanel.addMazeCellSelectionListener(mazeCellSelectionListener)
            disposer.addDisposeAction { UiController.mazePanel.removeMazeCellSelectionListener(mazeCellSelectionListener) }

            val kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager()
            val escDispatcher = DisposeOnEscape(disposer)
            kfm.addKeyEventDispatcher(escDispatcher)
            disposer.addDisposeAction { kfm.removeKeyEventDispatcher(escDispatcher) }

            val playerSelectionListener = object : PlayerSelectionListener {
                override fun onPlayerSelected(player: UiPlayerInformation) {
                    disposer.close()
                    // the disposer explicitly enables it, but in this case we need it disabled
                    putBaitButton.isEnabled = false
                }

                override fun onPlayerSelectionCleared() {
                    // irrelevant
                }
            }
            UiController.addPlayerSelectionListener(playerSelectionListener)
            disposer.addDisposeAction { UiController.removePlayerSelectionListener(playerSelectionListener) }

            val connectionStatusListener = object : ClientConnectionStatusListener {
                override fun onConnectionStatusChange(
                    oldStatus: ConnectionStatus,
                    newStatus: ConnectionStatus
                ) {
                    if (newStatus == ConnectionStatus.DEAD) {
                        disposer.close()
                    }
                }
            }
            UiController.client.eventHandler.addEventListener(connectionStatusListener)
            disposer.addDisposeAction { UiController.client.eventHandler.removeEventListener(connectionStatusListener) }
        }
        add(putBaitButton, SG_UNITY)
        add(baitTransformButton, SG_UNITY)

        // Bait rush
        val baitRushButton = JButton("Bait Rush")
        baitRushButton.mnemonic = KeyEvent.VK_B
        baitRushButton.addActionListener { _ ->
            baitRushButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.baitRush()
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    baitRushButton.isEnabled = true
                }
            }
        }
        add(baitRushButton, SPAN_2)
    }

    private fun initPlayerControlElements() {
        // Header
        val playerHeader = JLabel("Player control")
        playerHeader.font = playerHeader.font.deriveFont(Font.BOLD)
        add(playerHeader, SPAN_2)

        // Kill
        val killButton = object : JButton("Kill"), PlayerSelectionListener {
            override fun onPlayerSelected(player: UiPlayerInformation) {
                isEnabled = true
            }

            override fun onPlayerSelectionCleared() {
                isEnabled = false
            }
        }
        killButton.mnemonic = KeyEvent.VK_K
        UiController.addPlayerSelectionListener(killButton)
        killButton.isEnabled = false
        killButton.addActionListener { _ ->
            killButton.isEnabled = false
            serverController.launch {
                try {
                    val selectedPlayerIndex = UiController.scoreTable.selectedRow
                    val selectedPlayerId: Int? = UiController.uiPlayerCollection[selectedPlayerIndex]?.id
                    if (selectedPlayerId != null) {
                        if (selectedPlayerId == UiController.ownId) {
                            withContext(Dispatchers.Swing) {
                                JOptionPane.showMessageDialog(
                                    this@ServerControlPanel,
                                    "Killing yourself would result in connection loss and is therefore prevented.\nUse the 'Leave' button, if you are sure!",
                                    "Command not feasible",
                                    JOptionPane.WARNING_MESSAGE
                                )
                                killButton.isEnabled = true
                            }
                            return@launch
                        }
                        serverController.kill(selectedPlayerId)
                    }
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
            }
        }

        // Teleport
        val teleportButton = object : JButton("Teleport"), PlayerSelectionListener {
            override fun onPlayerSelected(player: UiPlayerInformation) {
                isEnabled = true
            }

            override fun onPlayerSelectionCleared() {
                isEnabled = false
            }
        }
        teleportButton.mnemonic = KeyEvent.VK_T
        UiController.addPlayerSelectionListener(teleportButton)
        teleportButton.isEnabled = false
        teleportButton.addActionListener { _ ->
            teleportButton.isEnabled = false
            killButton.isEnabled = false
            UiController.hintOnStatusBar("Select position")

            val disposer = Disposer()
            disposer.addDisposeAction {
                teleportButton.isEnabled = true
                killButton.isEnabled = true
                UiController.clearHintOnStatusBar()
            }

            val mazeCellSelectionListener = MazeCellSelectionListener { x, y ->
                val selectedPlayerIndex = UiController.scoreTable.selectedRow
                val selectedPlayerId: Int? = UiController.uiPlayerCollection[selectedPlayerIndex]?.id
                if (selectedPlayerId != null) {
                    serverController.launch {
                        try {
                            serverController.teleport(selectedPlayerId, x, y)
                        } catch (ex: ResponseException) {
                            withContext(Dispatchers.Swing) {
                                showErrorMessage(ex)
                            }
                        }
                        withContext(Dispatchers.Swing) {
                            disposer.close()
                        }
                    }
                }
            }
            UiController.mazePanel.addMazeCellSelectionListener(mazeCellSelectionListener)
            disposer.addDisposeAction { UiController.mazePanel.removeMazeCellSelectionListener(mazeCellSelectionListener) }

            val kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager()
            val escDispatcher = DisposeOnEscape(disposer)
            kfm.addKeyEventDispatcher(escDispatcher)
            disposer.addDisposeAction { kfm.removeKeyEventDispatcher(escDispatcher) }

            val playerSelectionListener = object : PlayerSelectionListener {
                override fun onPlayerSelected(player: UiPlayerInformation) {
                    // irrelevant
                }

                override fun onPlayerSelectionCleared() {
                    disposer.close()
                    // the disposer explicitly enables it, but in this case we need it disabled
                    teleportButton.isEnabled = false
                }
            }
            UiController.addPlayerSelectionListener(playerSelectionListener)
            disposer.addDisposeAction { UiController.removePlayerSelectionListener(playerSelectionListener) }

            val connectionStatusListener = object : ClientConnectionStatusListener {
                override fun onConnectionStatusChange(
                    oldStatus: ConnectionStatus,
                    newStatus: ConnectionStatus
                ) {
                    if (newStatus == ConnectionStatus.DEAD) {
                        disposer.close()
                        teleportButton.isEnabled = false
                    }
                }
            }
            UiController.client.eventHandler.addEventListener(connectionStatusListener)
            disposer.addDisposeAction { UiController.client.eventHandler.removeEventListener(connectionStatusListener) }
        }
        add(teleportButton)
        add(killButton)

        val playerInfo = PlayerInfoPanel()
        add(playerInfo, SPAN_2)
        UiController.addPlayerSelectionListener(playerInfo)

        val updateButton = object : JButton("Update"), PlayerSelectionListener {
            private var selectedId = -1

            init {
                isEnabled = false
                addActionListener { playerInfo.queryAndUpdate(selectedId) }
            }

            override fun onPlayerSelected(player: UiPlayerInformation) {
                isEnabled = true
                selectedId = player.id
            }

            override fun onPlayerSelectionCleared() {
                isEnabled = false
                selectedId = -1
            }
        }

        add(updateButton, SPAN_2)
        UiController.addPlayerSelectionListener(updateButton)

        // Spawn
        add(selectableNicks, SPAN_2)
        spawnButton.addActionListener {
            val selectedNick: String? = selectableNicks.selectedItem as String?
            if (selectedNick == null) {
                return@addActionListener
            }
            spawnButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.spawn(selectedNick)
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    spawnButton.isEnabled = true
                }
            }
        }
        add(spawnButton, SPAN_2)
    }

    private fun showErrorMessage(ex: ResponseException) {
        LOGGER.error(ex.message)
        JOptionPane.showMessageDialog(
            this@ServerControlPanel,
            ex.message,
            "Command failed",
            JOptionPane.ERROR_MESSAGE
        )
    }

    private inner class PlayerInfoPanel() : JPanel(), PlayerSelectionListener {
        private val nickLabel = JLabel("Nick")
        private val nickText = JLabel(NO_TEXT)
        private val idLabel = JLabel("ID")
        private val idText = JLabel(NO_TEXT)
        private val scoreLabel = JLabel("Score")
        private val scoreText = JLabel(NO_TEXT)
        private val serverSidedLabel = JLabel("Server-sided")
        private val serverSidedText = JLabel(NO_TEXT)
        private val delayOffsetLabel = JLabel("Delay offset")
        private val delayOffsetText = JLabel(NO_TEXT)
        private val playTimeTotalLabel = JLabel("Playtime (total)")
        private val playTimeTotalText = JLabel(NO_TEXT)
        private val playTimeCurrentLabel = JLabel("Playtime (current)")
        private val playTimeCurrentText = JLabel(NO_TEXT)
        private val ppmLabel = JLabel("Points per minute")
        private val ppmText = JLabel(NO_TEXT)
        private val msPerStepLabel = JLabel("ms/Step")
        private val msPerStepText = JLabel(NO_TEXT)

        init {
            layout = MigLayout("insets 5, wrap 2", "[grow,fill][grow,fill]")

            nickLabel.font = nickLabel.font.deriveFont(Font.BOLD)
            add(nickLabel, SG_UNITY)
            add(nickText, SG_UNITY)

            idLabel.font = idLabel.font.deriveFont(Font.BOLD)
            add(idLabel, SG_UNITY)
            add(idText, SG_UNITY)

            scoreLabel.font = scoreLabel.font.deriveFont(Font.BOLD)
            add(scoreLabel, SG_UNITY)
            add(scoreText, SG_UNITY)

            serverSidedLabel.font = serverSidedLabel.font.deriveFont(Font.BOLD)
            add(serverSidedLabel, SG_UNITY)
            add(serverSidedText, SG_UNITY)

            delayOffsetLabel.font = delayOffsetLabel.font.deriveFont(Font.BOLD)
            add(delayOffsetLabel, SG_UNITY)
            add(delayOffsetText, SG_UNITY)

            playTimeTotalLabel.font = playTimeTotalLabel.font.deriveFont(Font.BOLD)
            add(playTimeTotalLabel, SG_UNITY)
            add(playTimeTotalText, SG_UNITY)

            playTimeCurrentLabel.font = playTimeCurrentLabel.font.deriveFont(Font.BOLD)
            add(playTimeCurrentLabel, SG_UNITY)
            add(playTimeCurrentText, SG_UNITY)

            ppmLabel.font = ppmLabel.font.deriveFont(Font.BOLD)
            add(ppmLabel, SG_UNITY)
            add(ppmText, SG_UNITY)

            msPerStepLabel.font = msPerStepLabel.font.deriveFont(Font.BOLD)
            add(msPerStepLabel, SG_UNITY)
            add(msPerStepText, SG_UNITY)
        }

        fun clear() {
            nickText.text = NO_TEXT
            idText.text = NO_TEXT
            scoreText.text = NO_TEXT
            serverSidedText.text = NO_TEXT
            delayOffsetText.text = NO_TEXT
            playTimeTotalText.text = NO_TEXT
            playTimeCurrentText.text = NO_TEXT
            ppmText.text = NO_TEXT
            msPerStepText.text = NO_TEXT
        }

        private fun update(pi: PlayerInformationDto) {
            nickText.text = pi.nick
            idText.text = pi.id.toString()
            scoreText.text = pi.score.toString()
            serverSidedText.text = pi.serverSided.toString()
            delayOffsetText.text = pi.delayOffset.toString()
            playTimeTotalText.text = pi.totalPlayTime.time
            playTimeCurrentText.text = pi.currentPlayTime.time
            ppmText.text = pi.currentPointsPerMinute.toString()
            msPerStepText.text = pi.currentAvgMoveTimeInMs.toString()
        }

        override fun onPlayerSelected(player: UiPlayerInformation) {
            queryAndUpdate(player.id)
        }

        override fun onPlayerSelectionCleared() {
            UiController.uiScope.launch {
                clear()
            }
        }

        fun queryAndUpdate(id: Int) {
            if (!UiController.serverControllerActive) {
                // This case happens, if a previous connection had a server controller and the current one doesn't.
                return
            }
            serverController.launch {
                try {
                    val playerInformation: PlayerInformationDto = serverController.playerInformation(id)
                    withContext(Dispatchers.Swing) {
                        update(playerInformation)
                    }
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
            }
        }
    }
}