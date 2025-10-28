package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.system.exitProcess

class MainFrame() : JFrame(TITLE), ClientConnectionStatusListener, PlayerConnectionListener {
    companion object {
        private const val TITLE = "Maze-Game Client KT"
    }

    private val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
    private val connectionSettingsPanel = ConnectionSettingsPanel()
    private val leftSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    private val scorePanel: ScorePanel
    private lateinit var messagePane: MessagePane
    private val mazePanel: MazePanel
    private var connectionCounter: Int = 0
    private val leaveButton = JButton("Leave")
    private val statusBar = StatusBar()
    private var serverControlPanel: ServerControlPanel? = null
    private var serverControlPanelInPlace: Boolean = false

    init {
        UiController.mainFrame = this
        defaultCloseOperation = DO_NOTHING_ON_CLOSE

        // Fill the UI
        contentPane.layout = BorderLayout()
        contentPane.add(mainSplitPane, BorderLayout.CENTER)
        contentPane.add(statusBar, BorderLayout.SOUTH)

        val scoreTable = ScoreTable()
        scorePanel = ScorePanel()

        val messageScrollPane = createScrollableMessagePane()
        val messagePanel = JPanel()
        messagePanel.layout = BorderLayout()
        messagePanel.add(messageScrollPane, BorderLayout.CENTER)

        leaveButton.addActionListener { _ ->
            UiController.disconnect()
        }
        messagePanel.add(leaveButton, BorderLayout.SOUTH)
        leaveButton.isVisible = false
        leftSplitPane.add(connectionSettingsPanel, JSplitPane.TOP)
        leftSplitPane.add(messagePanel, JSplitPane.BOTTOM)
        leftSplitPane.resizeWeight = 0.4
        mainSplitPane.add(leftSplitPane, JSplitPane.LEFT)

        mazePanel = MazePanel()
        mazePanel.addMazeCellSelectionListener(scoreTable)
        mainSplitPane.add(mazePanel, JSplitPane.RIGHT)
        mainSplitPane.resizeWeight = 0.1


        val borderColor = UIManager.getColor("Separator.foreground")
        val fancyBorderTop = BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor)
        mainSplitPane.border = fancyBorderTop
        mainSplitPane.isOpaque = true

        val glassPane = GlassPane()
        UiController.glassPane = glassPane
        UiController.addPlayerSelectionListener(glassPane)
        this.glassPane = glassPane
        glassPane.isVisible = true

        UiController.prepareEventListener(this)

        // close operation
        addWindowListener(object : WindowAdapter() {

            override fun windowClosing(e: WindowEvent?) {
                UiController.bgScope.launch {
                    launch {
                        // Force exit after 5 seconds
                        delay(5000L)
                        exitProcess(0)
                    }
                    // clean disconnect and exit
                    UiController.onExit()
                    exitProcess(0)
                }
            }

        })

        // Size and position
        setSize(800, 600)
        isLocationByPlatform = true
        UiController.uiScope.launch {
            pack()
            isVisible = true
        }
    }

    fun createScrollableMessagePane(): JScrollPane {
        messagePane = MessagePane()
        messagePane.isEditable = false
        messagePane.preferredSize = Dimension(450, 300)
        messagePane.isVisible = false
        return JScrollPane(messagePane)
    }

    fun showOrHideServerControlPanel() {
        if (serverControlPanel == null) {
            serverControlPanel = ServerControlPanel()
        }
        if (serverControlPanelInPlace) {
            contentPane.remove(serverControlPanel)
            serverControlPanel?.isVisible = false
            serverControlPanelInPlace = false
        } else {
            contentPane.add(serverControlPanel!!, BorderLayout.EAST)
            serverControlPanel?.isVisible = true
            serverControlPanelInPlace = true
        }
        revalidate()
        repaint()
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        UiController.uiScope.launch {
            when (newStatus) {
                ConnectionStatus.LOGGED_IN -> {
                    leftSplitPane.remove(connectionSettingsPanel)
                    leftSplitPane.add(scorePanel, JSplitPane.TOP)
                    connectionCounter += 1
                    leaveButton.isVisible = true
                }

                ConnectionStatus.DEAD -> {
                    if (connectionCounter > 0) {
                        UiController.reset()

                        // The ui should enable a new connection
                        leftSplitPane.remove(scorePanel)
                        leftSplitPane.add(connectionSettingsPanel, JSplitPane.TOP)
                        leaveButton.isVisible = false
                        if (serverControlPanelInPlace) {
                            showOrHideServerControlPanel()
                        }
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