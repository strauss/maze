package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSplitPane

class MainFrame(private val controller: UiController) : JFrame(TITLE), ClientConnectionStatusListener {
    companion object {
        private const val TITLE = "Maze-Game Client KT"
    }

    private val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
    private val connectionSettingsPanel = ConnectionSettingsPanel(controller)
    private val dummyPanel = JPanel() // TODO: replace with the "Real thing"

    init {
        defaultCloseOperation = EXIT_ON_CLOSE

        // Fill the UI
        contentPane.layout = BorderLayout()
        mainSplitPane.add(connectionSettingsPanel, JSplitPane.LEFT)
        val emptyPanel = JPanel()
        emptyPanel.setSize(100, 200)
        mainSplitPane.add(emptyPanel, JSplitPane.RIGHT)
        contentPane.add(mainSplitPane, BorderLayout.CENTER)

        controller.prepareEventListener(this)

        // Size and position
        setSize(640, 480)
        isLocationByPlatform = true
        isVisible = true
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        controller.uiScope.launch {
            when (newStatus) {
                ConnectionStatus.LOGGED_IN -> {
                    mainSplitPane.remove(connectionSettingsPanel)
                    mainSplitPane.add(dummyPanel, JSplitPane.LEFT)
                }

                else -> {
                    // nothing
                }
            }
        }
    }
}