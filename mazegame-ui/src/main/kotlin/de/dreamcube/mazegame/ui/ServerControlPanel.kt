package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.common.maze.BaitType
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

class ServerControlPanel(private val controller: UiController) : JPanel() {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(ServerControlPanel::class.java)

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
        get() = controller.serverController ?: throw IllegalStateException("The server controller vanished.")

    init {
        layout = MigLayout("insets 5, wrap 2", "[grow,fill][grow,fill]")
        initGameControlElements()
        initBaitControlElements()
        initPlayerControlElements()
        // TODO: contest control

        val borderColor = UIManager.getColor("Separator.foreground")
        val fancyBorder = BorderFactory.createMatteBorder(1, 1, 0, 0, borderColor)
        border = fancyBorder
        isOpaque = true
    }

    private fun initGameControlElements() {
        // Header
        val gameHeader = JLabel("Game control")
        gameHeader.font = gameHeader.font.deriveFont(Font.BOLD)
        add(gameHeader, "span 2")

        // Go command
        val goButton = JButton("Go")
        goButton.mnemonic = KeyEvent.VK_G
        goButton.addActionListener { _ ->
            goButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.go()
                } catch (ex: ClientRequestException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    goButton.isEnabled = true
                }
            }
        }
        add(goButton, "sg unity")

        // Clear command
        val clearButton = JButton("Clear")
        clearButton.mnemonic = KeyEvent.VK_C
        clearButton.addActionListener { _ ->
            clearButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.clear()
                } catch (ex: ClientRequestException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    clearButton.isEnabled = true
                }
            }
        }
        add(clearButton, "sg unity")

        // Stop command
        val stopButton = JButton("Stop")
        stopButton.mnemonic = KeyEvent.VK_S
        stopButton.addActionListener { _ ->
            stopButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.stop()
                } catch (ex: ClientRequestException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    stopButton.isEnabled = true
                }
            }
        }
        add(stopButton, "sg unity")

        // Stop now command
        val stopNowButton = JButton("Stop now")
        stopNowButton.mnemonic = KeyEvent.VK_N
        stopNowButton.addActionListener { _ ->
            stopNowButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.stop(true)
                } catch (ex: ClientRequestException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    stopNowButton.isEnabled = true
                }
            }
        }
        add(stopNowButton, "sg unity")
    }

    private fun initBaitControlElements() {
        // Header
        val baitHeader = JLabel("Bait control")
        baitHeader.font = baitHeader.font.deriveFont(Font.BOLD)
        add(baitHeader, "span 2")

        // bait selection
        val baitTypeSelection = JComboBox<BaitType>()
        val model: DefaultComboBoxModel<BaitType?> = baitTypeSelection.model as DefaultComboBoxModel
        if (model.size > 0) {
            model.removeAllElements()
        }
        model.addAll(BaitType.entries)
        baitTypeSelection.selectedItem = BaitType.COFFEE
        baitTypeSelection.isEditable = false
        add(baitTypeSelection, "span 2")

        // Transform
        val baitTransformButton = JButton("Transform")
        baitTransformButton.addActionListener { _ ->
            val baitType: BaitType = baitTypeSelection.selectedItem as BaitType
            baitTransformButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.baitTransform(baitType)
                } catch (ex: ClientRequestException) {
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
        controller.addPlayerSelectionListener(putBaitButton)
        putBaitButton.mnemonic = KeyEvent.VK_P
        putBaitButton.addActionListener { _ ->
            putBaitButton.isEnabled = false
            controller.hintOnStatusBar("Select position")

            val disposer = Disposer()
            disposer.addDisposeAction {
                putBaitButton.isEnabled = true
                controller.clearHintOnStatusBar()
            }

            val mazeCellSelectionListener = MazeCellSelectionListener { x, y ->
                val baitType: BaitType = baitTypeSelection.selectedItem as BaitType
                serverController.launch {
                    try {
                        serverController.baitPut(baitType, x, y)
                    } catch (ex: ClientRequestException) {
                        withContext(Dispatchers.Swing) {
                            showErrorMessage(ex)
                        }
                    }
                    withContext(Dispatchers.Swing) {
                        disposer.close()
                    }
                }
            }
            controller.mazePanel.addMazeCellSelectionListener(mazeCellSelectionListener)
            disposer.addDisposeAction { controller.mazePanel.removeMazeCellSelectionListener(mazeCellSelectionListener) }

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
            controller.addPlayerSelectionListener(playerSelectionListener)
            disposer.addDisposeAction { controller.removePlayerSelectionListener(playerSelectionListener) }
        }
        add(putBaitButton, "sg unity")
        add(baitTransformButton, "sg unity")

        // Bait rush
        val baitRushButton = JButton("Bait Rush")
        baitRushButton.mnemonic = KeyEvent.VK_B
        baitRushButton.addActionListener { _ ->
            baitRushButton.isEnabled = false
            serverController.launch {
                try {
                    serverController.baitRush()
                } catch (ex: ClientRequestException) {
                    withContext(Dispatchers.Swing) {
                        showErrorMessage(ex)
                    }
                }
                withContext(Dispatchers.Swing) {
                    baitRushButton.isEnabled = true
                }
            }
        }
        add(baitRushButton, "span 2")
    }

    private fun initPlayerControlElements() {
        // Header
        val playerHeader = JLabel("Player control")
        playerHeader.font = playerHeader.font.deriveFont(Font.BOLD)
        add(playerHeader, "span 2")

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
        controller.addPlayerSelectionListener(killButton)
        killButton.isEnabled = false
        killButton.addActionListener { _ ->
            killButton.isEnabled = false
            serverController.launch {
                try {
                    val selectedPlayerIndex = controller.scoreTable.selectedRow
                    val selectedPlayerId: Int? = controller.uiPlayerCollection[selectedPlayerIndex]?.id
                    if (selectedPlayerId != null) {
                        if (selectedPlayerId == controller.ownId) {
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
                } catch (ex: ClientRequestException) {
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
        controller.addPlayerSelectionListener(teleportButton)
        teleportButton.isEnabled = false
        teleportButton.addActionListener { _ ->
            teleportButton.isEnabled = false
            killButton.isEnabled = false
            controller.hintOnStatusBar("Select position")

            val disposer = Disposer()
            disposer.addDisposeAction {
                teleportButton.isEnabled = true
                killButton.isEnabled = true
                controller.clearHintOnStatusBar()
            }

            val mazeCellSelectionListener = MazeCellSelectionListener { x, y ->
                val selectedPlayerIndex = controller.scoreTable.selectedRow
                val selectedPlayerId: Int? = controller.uiPlayerCollection[selectedPlayerIndex]?.id
                if (selectedPlayerId != null) {
                    serverController.launch {
                        try {
                            serverController.teleport(selectedPlayerId, x, y)
                        } catch (ex: ClientRequestException) {
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
            controller.mazePanel.addMazeCellSelectionListener(mazeCellSelectionListener)
            disposer.addDisposeAction { controller.mazePanel.removeMazeCellSelectionListener(mazeCellSelectionListener) }

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
            controller.addPlayerSelectionListener(playerSelectionListener)
            disposer.addDisposeAction { controller.removePlayerSelectionListener(playerSelectionListener) }
        }
        add(teleportButton)
        add(killButton)

        // Player info
        // TODO: Make it so

        // Spawn
        // TODO: Make it so

    }

    private fun showErrorMessage(ex: ClientRequestException) {
        LOGGER.error(ex.message)
        JOptionPane.showMessageDialog(
            this@ServerControlPanel,
            ex.message,
            "Command failed",
            JOptionPane.ERROR_MESSAGE
        )
    }
}