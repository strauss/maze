package de.dreamcube.mazegame.client.maze.strategy

import de.dreamcube.mazegame.client.maze.events.NoEventListener
import java.awt.Color
import java.awt.Graphics
import java.awt.Point
import javax.swing.JComponent

/**
 * Abstract class for representing bot visualizations. Will automatically be registered as
 * [de.dreamcube.mazegame.client.maze.events.EventListener] for client events. Just implement the listener interfaces
 * of events you are interested in, and their corresponding functions. See [Strategy] for a more detailed explanation.
 */
abstract class VisualizationComponent : JComponent(), NoEventListener {

    /**
     * Should the visualization be activated right away?
     */
    open val activateImmediately: Boolean
        get() = false

    /**
     * Is the visualization active or not. Should only be set by the UI.
     */
    var visualizationEnabled = activateImmediately

    /**
     * This map contains the color distribution for all player ids. Should only be set by the UI.
     */
    var colorDistributionMap: Map<Int, Color> = mapOf()

    /**
     * The id of the selected player. Can be used for reacting to player selections in the visualization. Should only be
     * set by the UI.
     */
    var selectedPlayerId: Int? = null
        set(value) {
            field = value
            repaint()
        }

    /**
     * The current zoom factor of the maze. Should only be set by the UI.
     */
    var zoom = 1
        set(value) {
            field = value
            repaint()
        }

    /**
     * The offset of the origin point (top left corner).
     */
    protected val offset: Point = Point(0, 0)

    /**
     * Update function for the offset. Should only be called by the UI
     */
    fun updateOffset(x: Int, y: Int) {
        offset.x = x
        offset.y = y
        repaint()
    }

    /**
     * Use this method to retrieve a player's color.
     */
    fun getPlayerColor(playerId: Int): Color {
        return colorDistributionMap[playerId] ?: Color.BLACK
    }

    /**
     * Override this function to actually paint your visualization.
     */
    protected override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
    }

}