/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
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
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.system.exitProcess

/**
 * This class resembles the main frame of the whole application.
 */
class MainFrame() : JFrame(TITLE), ClientConnectionStatusListener, PlayerConnectionListener {
    companion object {
        private const val TITLE = "Maze-Game Client KT"
        private const val VIS_BUTTON_TEXT_ON = "Visualization: ON"
        private const val VIS_BUTTON_TEXT_OFF = "Visualization: OFF"
        private const val VIS_POS = 1
        private const val MARK_POS = 2
    }

    /**
     * The [JSplitPane] separating the maze visualization (right) from the score table and chat (left).
     */
    private val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)

    /**
     * The panel for configuring the connection.
     */
    private val connectionSettingsPanel = ConnectionSettingsPanel()

    /**
     * The split [JSplitPane] separating the score table and the chat.
     */
    private val leftSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)

    /**
     * The [ScorePanel] including the [ScoreTable].
     */
    private val scorePanel: ScorePanel

    /**
     * The chat.
     */
    private lateinit var messagePane: MessagePane

    /**
     * The maze visualization.
     */
    private val mazePanel: MazePanel

    /**
     * Counts the number of connections the application has performed until now.
     */
    private var connectionCounter: Int = 0

    /**
     * The leave button.
     */
    private val leaveButton = JButton("Leave")

    /**
     * The [StatusBar] of the application, including certain information about the game. It also contains the control
     * button.
     */
    private val statusBar = StatusBar()

    /**
     * The control area on the right side.
     */
    private val controlPane = JPanel()

    /**
     * The [JPanel] containing the bot control, if the strategy provides it.
     */
    private val botControlPanel = JPanel()

    /**
     * The [ServerControlPanel] allowing for controlling the game server, if the control connection was established
     * before connecting to the game.
     */
    private var serverControlPanel: ServerControlPanel? = null

    /**
     * Flag indicating, if the control panel is active.
     */
    private var controlPanelInPlace: Boolean = false

    /**
     * The glass pane containing the marker panel and the bot visualization if the strategy provides it.
     */
    private val layeredGlassPane: JLayeredPane

    /**
     * This button is only visible, if the selected strategy provides a visualization. If it does, it is used to toggle
     * it.
     */
    private inner class VisualizationButton : JButton(VIS_BUTTON_TEXT_OFF) {
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

        /**
         * Activates the visualization.
         */
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

        /**
         * Deactivates the visualization.
         */
        fun deactivate() {
            UiController.visualizationComponent?.let {
                it.visualizationEnabled = false
                layeredGlassPane.remove(it)
            }
            on = false
            text = VIS_BUTTON_TEXT_OFF
        }
    }

    private val visualizationButton = VisualizationButton()

    init {
        addIcons()
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
        UiController.addMazeCellListener(scoreTable)
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

            /**
             * Limits the visible area of the layered glass pane to the [mazePanel]'s visible area.
             */
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

    /**
     * Sets up the chat view.
     */
    private fun createScrollableMessagePane(): JScrollPane {
        messagePane = MessagePane()
        messagePane.isEditable = false
        messagePane.preferredSize = Dimension(450, 300)
        return JScrollPane(messagePane)
    }

    /**
     * Toggles the control panel.
     */
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

    /**
     * Reacts to connection status change events.
     *
     * - [ConnectionStatus.CONNECTED]: enables/activates visualization and bot control if the strategy provides it. Also
     * enables the control button on the status bar.
     * - [ConnectionStatus.LOGGED_IN]: replaces the connection panel with the score panel, activates the leave button,
     * and counts a successful connection.
     * - [ConnectionStatus.PLAYING]: tells the [UiController] that we are playing (effectively initializes the own
     * player id in the [UiController]).
     * - [ConnectionStatus.DEAD]: If we had at least one successful connection, the whole UI is reset into a state that
     * it can establish a new connection. Only bad programs don't allow for reusing the UI and force the user to
     * restart the whole application...
     */
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
                        if (visualizationComponent.activateImmediately) {
                            visualizationButton.activate()
                        }
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

    /**
     * Loads the application icon in different sizes from the resources.
     */
    private fun addIcons() {
        iconImages = buildList {
            add(ImageIO.read(this@MainFrame.javaClass.getResource("16x16.png")))
            add(ImageIO.read(this@MainFrame.javaClass.getResource("32x32.png")))
            add(ImageIO.read(this@MainFrame.javaClass.getResource("48x48.png")))
        }
    }
}