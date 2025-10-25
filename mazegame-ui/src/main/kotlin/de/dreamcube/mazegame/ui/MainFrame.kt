package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class MainFrame(private val controller: UiController) : JFrame(TITLE), ClientConnectionStatusListener, PlayerConnectionListener {
    companion object {
        private const val TITLE = "Maze-Game Client KT"
    }

    private val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
    private val connectionSettingsPanel = ConnectionSettingsPanel(controller)
    private val leftSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    private val scorePanel = JPanel()
    private val scoreTable: ScoreTable
    private lateinit var messagePane: MessagePane
    private val mazePanel: MazePanel
    private var connectionCounter: Int = 0
    private val leaveButton = JButton("Leave")

    init {
        defaultCloseOperation = EXIT_ON_CLOSE

        // Fill the UI
        contentPane.layout = BorderLayout()
        contentPane.add(mainSplitPane, BorderLayout.CENTER)

        scorePanel.layout = BorderLayout()
        scoreTable = ScoreTable(controller)
        val tableScrollPane = JScrollPane(scoreTable)
        scorePanel.add(tableScrollPane)

        val messageScrollPane = createScrollableMessagePane()
        val messagePanel = JPanel()
        messagePanel.layout = BorderLayout()
        messagePanel.add(messageScrollPane, BorderLayout.CENTER)

        leaveButton.addActionListener { _ ->
            controller.disconnect()
        }
        messagePanel.add(leaveButton, BorderLayout.SOUTH)
        leaveButton.isVisible = false
        leftSplitPane.add(connectionSettingsPanel, JSplitPane.TOP)
        leftSplitPane.add(messagePanel, JSplitPane.BOTTOM)
        leftSplitPane.resizeWeight = 0.4
        mainSplitPane.add(leftSplitPane, JSplitPane.LEFT)

        mazePanel = MazePanel(controller)
        mainSplitPane.add(mazePanel, JSplitPane.RIGHT)
        mainSplitPane.resizeWeight = 0.1

        controller.mazePanel = mazePanel

        controller.prepareEventListener(this)

        // Size and position
        setSize(640, 480)
        isLocationByPlatform = true
        controller.uiScope.launch {
            pack()
            isVisible = true
        }
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
                    leftSplitPane.remove(connectionSettingsPanel)
                    leftSplitPane.add(scorePanel, JSplitPane.TOP)
                    connectionCounter += 1
                    leaveButton.isVisible = true
                }

                ConnectionStatus.DEAD -> {
                    if (connectionCounter > 0) {
                        mazePanel.reset()
                        messagePane.reset()
                        scoreTable.reset()

                        // The ui should enable a new connection
                        leftSplitPane.remove(scorePanel)
                        leftSplitPane.add(connectionSettingsPanel, JSplitPane.TOP)
                        leaveButton.isVisible = false
                        repaint()
                    }
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