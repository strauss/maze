package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane

class MainFrame(private val controller: UiController) : JFrame(TITLE), ClientConnectionStatusListener, PlayerConnectionListener {
    companion object {
        private const val TITLE = "Maze-Game Client KT"
    }

    private val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
    private val connectionSettingsPanel = ConnectionSettingsPanel(controller)
    private val leftSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    private val scorePanel = JPanel()
    val scoreTable: ScoreTable
    lateinit var messagePane: MessagePane

    init {
        defaultCloseOperation = EXIT_ON_CLOSE

        // Fill the UI
        contentPane.layout = BorderLayout()
        mainSplitPane.add(connectionSettingsPanel, JSplitPane.LEFT)
        val emptyPanel = JPanel()
        emptyPanel.setSize(100, 200)
        mainSplitPane.add(emptyPanel, JSplitPane.RIGHT)
        contentPane.add(mainSplitPane, BorderLayout.CENTER)

        scorePanel.layout = BorderLayout()
        scoreTable = ScoreTable(controller)
        val tableScrollPane = JScrollPane(scoreTable)
        scorePanel.add(tableScrollPane)
        leftSplitPane.add(scorePanel)
        leftSplitPane.add(createScrollableMessagePane(), JSplitPane.BOTTOM)
        leftSplitPane.resizeWeight = 0.4

        controller.prepareEventListener(this)

        // Size and position
        setSize(640, 480)
        isLocationByPlatform = true
        pack()
        isVisible = true
    }

    fun createScrollableMessagePane(): JScrollPane {
        messagePane = MessagePane(controller)
        messagePane.isEditable = false
        messagePane.preferredSize = Dimension(160, 300)
        messagePane.isVisible = false
        return JScrollPane(messagePane)
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        controller.uiScope.launch {
            when (newStatus) {
                ConnectionStatus.LOGGED_IN -> {
                    mainSplitPane.remove(connectionSettingsPanel)
                    mainSplitPane.add(leftSplitPane, JSplitPane.LEFT)
                    pack()
                }

                else -> {
                    // nothing
                }
            }
        }
    }

    override fun onPlayerLogin(playerSnapshot: PlayerSnapshot) {
        leftSplitPane.resetToPreferredSizes()
    }

    override fun onOwnPlayerLogin(playerSnapshot: PlayerSnapshot) {
        messagePane.isVisible = true
    }

    override fun onPlayerLogout(playerSnapshot: PlayerSnapshot) {
        leftSplitPane.resetToPreferredSizes()
    }
}