package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.common.maze.BaitType
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import net.miginfocom.swing.MigLayout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Font
import javax.swing.*

class ServerControlPanel(private val controller: UiController) : JPanel() {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(ServerControlPanel::class.java)
    }

    private val serverController: ServerCommandController
        get() = controller.serverController ?: throw IllegalStateException("The server controller vanished.")

    init {
        layout = MigLayout("insets 5, wrap 2", "[grow,fill][grow,fill]")
        initGameControlElements()
        initBaitControlElements()
    }

    private fun initGameControlElements() {
        // Header
        val gameHeader = JLabel("Game control")
        gameHeader.font = gameHeader.font.deriveFont(Font.BOLD)
        add(gameHeader, "span 2")

        // Go command
        val goButton = JButton("Go")
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
        add(baitTransformButton, "sg unity")

        // Put bait
        val putBaitButton = JButton("Put")
        putBaitButton.isEnabled = false
        // TODO implement it
        add(putBaitButton, "sg unity")

        // Bait rush
        val baitRushButton = JButton("Bait Rush")
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

    private fun showErrorMessage(ex: ClientRequestException) {
        JOptionPane.showMessageDialog(
            this@ServerControlPanel,
            ex.message,
            "Command failed",
            JOptionPane.ERROR_MESSAGE
        )
    }
}