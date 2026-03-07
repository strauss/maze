/*
 * Maze Game
 * Copyright (c) 2025-2026 Sascha Strauß
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
import de.dreamcube.mazegame.common.api.*
import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.util.Disposer
import io.ktor.client.plugins.*
import io.ktor.http.*
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
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds

private const val SG_UNITY = "sg unity"
private const val SPAN_2 = "span 2"

/**
 * The server control panel containing all UI elements for the server control.
 */
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

    /**
     * The [ServerCommandController] for the "heavy lifting"
     */
    private val serverController: ServerCommandController
        get() = UiController.serverController ?: throw IllegalStateException("The server controller vanished.")

    /**
     * Selector for selecting a server-sided bot strategy to spawn.
     */
    private val selectableNicks = JComboBox<String>()

    /**
     * The spawn button.
     */
    private val spawnButton = JButton("Spawn")

    /**
     * The list of server-side bots to spawn.
     */
    lateinit var availableServersideNicks: List<String>

    init {
        layout = MigLayout("insets 5, wrap 2", "[grow,fill][grow,fill]")
        initGameControlElements()
        initBaitControlElements()
        initPlayerControlElements()
        initConstestControlElements()

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

        val speedLabel = JLabel("Speed control")
        speedLabel.font = speedLabel.font.deriveFont(Font.BOLD)
        add(speedLabel, SPAN_2)

        // speed selection
        val speedSelection = speedSelection()
        add(speedSelection, SPAN_2)

        // speed selection button
        val selectSpeedButton = JButton("Change speed")
        selectSpeedButton.addActionListener { _ ->
            val selectedSpeed: GameSpeed = speedSelection.selectedItem as GameSpeed
            selectSpeedButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.changeSpeed(selectedSpeed)
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    selectSpeedButton.isEnabled = true
                }
            }
        }
        add(selectSpeedButton, SPAN_2)
    }

    private fun speedSelection(): JComboBox<GameSpeed> {
        val speedSelection = JComboBox<GameSpeed>()
        val model: DefaultComboBoxModel<GameSpeed?> = speedSelection.model as DefaultComboBoxModel
        if (model.size > 0) {
            model.removeAllElements()
        }
        model.addAll(GameSpeed.entries)
        speedSelection.selectedItem = GameSpeed.NORMAL
        speedSelection.isEditable = false
        return speedSelection
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
            UiController.activateHoverMarks()

            val disposer = Disposer()
            disposer.addDisposeAction {
                putBaitButton.isEnabled = true
                UiController.clearHintOnStatusBar()
                UiController.deactivateHoverMarks()
            }

            val mazeCellListener = MazeCellListener { x, y, _ ->
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
            UiController.addMazeCellListener(mazeCellListener)
            disposer.addDisposeAction {
                UiController.removeMazeCellListener(mazeCellListener)
            }

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
            UiController.activateHoverMarks()

            val disposer = Disposer()
            disposer.addDisposeAction {
                teleportButton.isEnabled = true
                killButton.isEnabled = true
                UiController.clearHintOnStatusBar()
                UiController.deactivateHoverMarks()
            }

            val mazeCellListener = MazeCellListener { x, y, _ ->
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
            UiController.addMazeCellListener(mazeCellListener)
            disposer.addDisposeAction {
                UiController.removeMazeCellListener(mazeCellListener)
            }

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

        val playerSelectionLabel = JLabel("Spawn control")
        playerSelectionLabel.font = playerSelectionLabel.font.deriveFont(Font.BOLD)
        add(playerSelectionLabel, SPAN_2)

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

    private fun initConstestControlElements() {
        val contestControlButton = JButton("Contest Control")
        add(contestControlButton)

        contestControlButton.addActionListener {

            serverController.launch {
                var contestConfiguration: ContestConfiguration? = null
                try {
                    contestConfiguration = serverController.contestInfo()

                } catch (ex: ResponseException) {
                    if (ex.response.status != HttpStatusCode.NotFound) {
                        withContext(Dispatchers.Swing) {
                            showErrorMessage(ex)
                        }
                        return@launch
                    }
                }
                withContext(Dispatchers.Swing) {
                    if (contestConfiguration == null) {
                        showContestLauncherDialog()
                    } else {
                        showRunningContestConfiguration(contestConfiguration)
                    }
                }
            }
        }

        val contestReportButton = JButton("Contest Report")
        contestReportButton.addActionListener {
            serverController.launch {
                try {
                    serverController.contestReport()
                } catch (ex: ResponseException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
            }
        }

        add(contestReportButton)
    }

    private fun showContestLauncherDialog() {
        val setupContestDialog = JDialog(UiController.mainFrame, "Setup new Contest", true)
        setupContestDialog.run {
            layout = MigLayout("insets 5, wrap 2", "[grow,fill][grow,fill]")

            add(JLabel("Duration (minutes)"), SG_UNITY)
            val durationInMinutesField = JTextField("30.0")
            add(durationInMinutesField, SG_UNITY)

            add(JLabel("Show positions"), SG_UNITY)
            val showPositionsField = JTextField("10")
            add(showPositionsField, SG_UNITY)

            add(JLabel("Initial speed"), SG_UNITY)
            val speedSelection = speedSelection()
            add(speedSelection, SG_UNITY)

            val frenzyBot = JCheckBox("Frenzy bot", true)
            add(frenzyBot, SPAN_2)

            val speedUp = JCheckBox("Speed up", true)
            add(speedUp, SPAN_2)

            val baitRush = JCheckBox("Bait rush", true)
            add(baitRush, SPAN_2)

            val shuffleAndReBait = JCheckBox("Shuffle and re-bait", true)
            add(shuffleAndReBait, SPAN_2)

            val cancelButton = JButton("Close")
            cancelButton.addActionListener {
                this.dispose()
            }
            add(cancelButton, SG_UNITY)

            val startButton = JButton("Start")
            startButton.font = startButton.font.deriveFont(Font.BOLD)
            startButton.addActionListener {
                serverController.launch {
                    try {
                        val configuration = ContestConfiguration(
                            durationInMinutes = durationInMinutesField.text.toDouble(),
                            statusPositions = showPositionsField.text.toInt(),
                            initialGameSpeed = speedSelection.selectedItem as GameSpeed,
                            eventSets = buildSet {
                                if (frenzyBot.isSelected) {
                                    add(CuratedEventSet.DEFAULT_FRENZY_MODE)
                                }
                                if (speedUp.isSelected) {
                                    add(CuratedEventSet.SPEED_UP)
                                }
                                if (baitRush.isSelected) {
                                    add(CuratedEventSet.BAIT_RUSH)
                                }
                                if (shuffleAndReBait.isSelected) {
                                    add(CuratedEventSet.SHUFFLE_AND_REBAIT)
                                }
                            }
                        )
                        serverController.startContest(configuration)
                        withContext(Dispatchers.Swing) {
                            this@run.dispose()
                        }
                    } catch (ex: ResponseException) {
                        withContext(Dispatchers.Swing) {
                            showErrorMessage(ex)
                            this@run.dispose()
                        }
                    } catch (ex: NumberFormatException) {
                        showErrorMessage(ex)
                        // do not dispose in this case
                    }
                }
            }
            add(startButton, SG_UNITY)

            pack()
            setLocationRelativeTo(UiController.mainFrame)
            isVisible = true
        }
    }

    private fun showRunningContestConfiguration(configuration: ContestConfiguration) {
        val runningContestDialog = JDialog(UiController.mainFrame, "Running Contest", true)
        fun readOnlyTextFieldWithContent(content: String) = JTextField(content).apply { isEnabled = false }
        fun yesOrNo(flag: Boolean) = if (flag) "Yes" else "No"
        runningContestDialog.run {
            layout = MigLayout("insets 5, wrap 2", "[grow,fill][grow,fill]")

            add(JLabel("Duration"), SG_UNITY)
            val durationString = round(configuration.durationInMinutes * 60.0 * 1000.0).milliseconds.toString()
            add(readOnlyTextFieldWithContent(durationString), SG_UNITY)

            add(JLabel("Report interval"), SG_UNITY)
            val reportIntervalString =
                round(configuration.statusReportIntervalInMinutes * 60.0 * 1000.0).milliseconds.toString()
            add(readOnlyTextFieldWithContent(reportIntervalString), SG_UNITY)

            add(JLabel("Show positions"), SG_UNITY)
            add(readOnlyTextFieldWithContent(configuration.statusPositions.toString()), SG_UNITY)

            add(JLabel("Initial speed"), SG_UNITY)
            add(readOnlyTextFieldWithContent(configuration.initialGameSpeed.shortName), SG_UNITY)

            add(JLabel("Frenzy bot"), SG_UNITY)
            add(
                readOnlyTextFieldWithContent(yesOrNo(CuratedEventSet.DEFAULT_FRENZY_MODE in configuration.eventSets)),
                SG_UNITY
            )

            add(JLabel("Speed up"), SG_UNITY)
            add(readOnlyTextFieldWithContent(yesOrNo(CuratedEventSet.SPEED_UP in configuration.eventSets)), SG_UNITY)

            add(JLabel("Bait rush"), SG_UNITY)
            add(readOnlyTextFieldWithContent(yesOrNo(CuratedEventSet.BAIT_RUSH in configuration.eventSets)), SG_UNITY)

            add(JLabel("Shuffle and re-bait"), SG_UNITY)
            add(
                readOnlyTextFieldWithContent(yesOrNo(CuratedEventSet.SHUFFLE_AND_REBAIT in configuration.eventSets)),
                SG_UNITY
            )

            add(JLabel("Additional events"), SG_UNITY)
            add(readOnlyTextFieldWithContent(yesOrNo(configuration.additionalEvents.isNotEmpty())), SG_UNITY)

            val okButton = JButton("Close")
            okButton.addActionListener {
                this.dispose()
            }
            add(okButton, SG_UNITY)

            val stopButton = JButton("Stop")
            stopButton.font = stopButton.font.deriveFont(Font.BOLD)
            stopButton.addActionListener {
                val result: Int = JOptionPane.showConfirmDialog(
                    this,
                    "Do you really want to stop the running contest?",
                    "Question",
                    JOptionPane.YES_NO_CANCEL_OPTION
                )

                if (result == JOptionPane.YES_OPTION) {
                    serverController.launch {
                        try {
                            serverController.stopContest()
                            withContext(Dispatchers.Swing) {
                                this@run.dispose()
                            }
                        } catch (ex: ResponseException) {
                            withContext(Dispatchers.Swing) {
                                showErrorMessage(ex)
                            }
                        }
                    }
                } else if (result == JOptionPane.NO_OPTION) {
                    this.dispose()
                }
                // in case of CANCEL_OPTION we just leave the dialog open ... effectively doing nothing at this point
            }
            add(stopButton, SG_UNITY)

            pack()
            setLocationRelativeTo(UiController.mainFrame)
            isVisible = true
        }

    }

    private fun showErrorMessage(ex: ResponseException) {
        val message = ex.message
        showErrorMessage(message)
    }

    private fun showErrorMessage(ex: NumberFormatException) {
        val message = "Malformed number ${ex.message}"
        showErrorMessage(message)
    }

    private fun showErrorMessage(message: String?) {
        LOGGER.error(message)
        JOptionPane.showMessageDialog(
            this@ServerControlPanel,
            message,
            "Command failed",
            JOptionPane.ERROR_MESSAGE
        )
    }

    /**
     * Info panel for the server-sided player information.
     */
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