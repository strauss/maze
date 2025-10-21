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
    val scoreTable: ScoreTable
    lateinit var messagePane: MessagePane
    lateinit var mazePanel: MazePanel
    private var connectionCounter: Int = 0

    init {
        defaultCloseOperation = EXIT_ON_CLOSE

        // Fill the UI
        contentPane.layout = BorderLayout()
        mainSplitPane.add(connectionSettingsPanel, JSplitPane.LEFT)
        contentPane.add(mainSplitPane, BorderLayout.CENTER)

        scorePanel.layout = BorderLayout()
        scoreTable = ScoreTable(controller)
        val tableScrollPane = JScrollPane(scoreTable)
        scorePanel.add(tableScrollPane)
        leftSplitPane.add(scorePanel)
        val messageScrollPane = createScrollableMessagePane()
        val messagePanel = JPanel()
        messagePanel.layout = BorderLayout()
        messagePanel.add(messageScrollPane, BorderLayout.CENTER)
        val leaveButton = JButton("Leave")
        leaveButton.addActionListener { _ ->
            controller.disconnect()
        }
        messagePanel.add(leaveButton, BorderLayout.SOUTH)
        leftSplitPane.add(messagePanel, JSplitPane.BOTTOM)
        leftSplitPane.resizeWeight = 0.4

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
                    mainSplitPane.remove(connectionSettingsPanel)
                    mainSplitPane.add(leftSplitPane, JSplitPane.LEFT)
                    if (connectionCounter < 1) {
                        // On first login, we need to create and setup the maze panel
                        mazePanel = MazePanel(controller)
                        mainSplitPane.add(mazePanel, JSplitPane.RIGHT)
                        controller.mazePanel = mazePanel
                    }
                    connectionCounter += 1
                    pack()
                }

                ConnectionStatus.DEAD -> {
                    if (connectionCounter > 0) {
                        mazePanel.reset()
                        messagePane.reset()
                        scoreTable.reset()

                        // The ui should enable a new connection
                        mainSplitPane.remove(leftSplitPane)
                        mainSplitPane.add(connectionSettingsPanel)
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