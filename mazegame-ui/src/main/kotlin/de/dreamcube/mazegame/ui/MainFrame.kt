package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.client.maze.strategy.VisualizationComponent
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.*
import javax.swing.*
import kotlin.system.exitProcess

class MainFrame() : JFrame(TITLE), ClientConnectionStatusListener, PlayerConnectionListener {
    companion object {
        private const val TITLE = "Maze-Game Client KT"
        private const val VIS_BUTTON_TEXT_ON = "Visualization: ON"
        private const val VIS_BUTTON_TEXT_OFF = "Visualization: OFF"
        private const val VIS_POS = 1
        private const val MARK_POS = 2
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
    private val controlPane = JPanel()
    private val botControlPanel = JPanel()
    private var serverControlPanel: ServerControlPanel? = null
    private var controlPanelInPlace: Boolean = false
    private val layeredGlassPane: JLayeredPane

    private val visualizationButton = object : JButton(VIS_BUTTON_TEXT_OFF) {
        val visualizationComponent: VisualizationComponent?
            get() = UiController.client.strategy.getVisualizationComponent()

        var on: Boolean
            get() = visualizationComponent?.visualizationEnabled ?: false
            set(value) {
                visualizationComponent?.visualizationEnabled = value
            }

        init {
            addActionListener { _ ->
                if (on)
                    deactivate()
                else
                    activate()
            }
        }

        fun activate() {
            UiController.client.strategy.getVisualizationComponent()?.let { visualizationComponent ->
                visualizationComponent.zoom = UiController.mazePanel.zoom
                val offset = UiController.mazePanel.offset
                visualizationComponent.updateOffset(offset.x, offset.y)
                layeredGlassPane.add(visualizationComponent, VIS_POS)
                layeredGlassPane.repaint()
                on = true
                text = VIS_BUTTON_TEXT_ON
            }
        }

        fun deactivate() {
            UiController.visualizationComponent?.let {
                it.visualizationEnabled = false
                layeredGlassPane.remove(it)
            }
            on = false
            text = VIS_BUTTON_TEXT_OFF
        }
    }

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

        controlPane.layout = BorderLayout()
        val borderColor = UIManager.getColor("Separator.foreground")
        val fancyBorderControlPane = BorderFactory.createMatteBorder(1, 1, 0, 0, borderColor)
        controlPane.border = fancyBorderControlPane
        botControlPanel.layout = BorderLayout()

        val fancyBorderTop = BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor)
        mainSplitPane.border = fancyBorderTop
        mainSplitPane.isOpaque = true

        layeredGlassPane = object : JLayeredPane() {
            init {
                isOpaque = false
            }

            override fun doLayout() {
                val translatedMazePanelRectangle =
                    SwingUtilities.convertRectangle(mazePanel, Rectangle(0, 0, mazePanel.width, mazePanel.height), this)
                for (component in components) {
                    component.bounds = translatedMazePanelRectangle
                }
            }

            override fun contains(x: Int, y: Int) = false
        }

        fun updateLayeredGlassPane() {
            layeredGlassPane.revalidate()
            layeredGlassPane.repaint()
        }

        val mazePanelUpdateListener = object : ComponentAdapter(), HierarchyBoundsListener {
            override fun componentResized(e: ComponentEvent?) {
                updateLayeredGlassPane()
            }

            override fun ancestorResized(e: HierarchyEvent?) {
                updateLayeredGlassPane()
            }

            override fun componentMoved(e: ComponentEvent?) {
                updateLayeredGlassPane()
            }

            override fun ancestorMoved(e: HierarchyEvent?) {
                updateLayeredGlassPane()
            }
        }
        mazePanel.addComponentListener(mazePanelUpdateListener)
        mazePanel.addHierarchyBoundsListener(mazePanelUpdateListener)

        val markerPane = MarkerPane()
        UiController.markerPane = markerPane
        UiController.addPlayerSelectionListener(markerPane)
        layeredGlassPane.add(markerPane, MARK_POS)
        this.glassPane = layeredGlassPane
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

    private fun createScrollableMessagePane(): JScrollPane {
        messagePane = MessagePane()
        messagePane.isEditable = false
        messagePane.preferredSize = Dimension(450, 300)
        return JScrollPane(messagePane)
    }

    internal fun showOrHideControlPanel() {
        if (controlPanelInPlace) {
            contentPane.remove(controlPane)
            controlPane.isVisible = false
            controlPanelInPlace = false
        } else {
            contentPane.add(controlPane, BorderLayout.EAST)
            controlPane.isVisible = true
            controlPanelInPlace = true
        }
        revalidate()
        repaint()
    }

    internal fun initServerControlPanel() {
        if (serverControlPanel == null) {
            serverControlPanel = ServerControlPanel()
        }
        controlPane.add(serverControlPanel!!, BorderLayout.NORTH)
    }

    internal fun clearControlPanel() {
        botControlPanel.removeAll()
        controlPane.removeAll()
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        UiController.uiScope.launch {
            when (newStatus) {
                ConnectionStatus.CONNECTED -> {
                    val strategy: Strategy = UiController.client.strategy
                    var botControlActive = false
                    strategy.getControlPanel()?.let {
                        botControlPanel.add(it, BorderLayout.CENTER)
                        botControlActive = true
                    }
                    strategy.getVisualizationComponent()?.let { visualizationComponent ->
                        botControlPanel.add(visualizationButton, BorderLayout.SOUTH)
                        UiController.visualizationComponent = visualizationComponent
                        botControlActive = true
                    }
                    if (botControlActive) {
                        controlPane.add(botControlPanel, BorderLayout.CENTER)
                        UiController.activateControlButton()
                    }
                }

                ConnectionStatus.LOGGED_IN -> {
                    leftSplitPane.remove(connectionSettingsPanel)
                    leftSplitPane.add(scorePanel, JSplitPane.TOP)
                    connectionCounter += 1
                    leaveButton.isVisible = true
                }

                ConnectionStatus.PLAYING -> {
                    UiController.startPlaying()
                }

                ConnectionStatus.DEAD -> {
                    if (connectionCounter > 0) {
                        UiController.reset()
                        // remove visualization
                        UiController.visualizationComponent?.let { layeredGlassPane.remove(it) }
                        visualizationButton.text = VIS_BUTTON_TEXT_OFF
                        UiController.visualizationComponent = null
                        // The ui should enable a new connection
                        leftSplitPane.remove(scorePanel)
                        leftSplitPane.add(connectionSettingsPanel, JSplitPane.TOP)
                        leaveButton.isVisible = false
                        clearControlPanel()
                        if (controlPanelInPlace) {
                            showOrHideControlPanel()
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

    override fun onPlayerLogout(playerSnapshot: PlayerSnapshot) {
        leftSplitPane.resetToPreferredSizes()
    }
}