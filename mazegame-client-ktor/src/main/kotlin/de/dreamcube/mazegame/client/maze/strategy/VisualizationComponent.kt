package de.dreamcube.mazegame.client.maze.strategy

import de.dreamcube.mazegame.client.maze.events.NoEventListener
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
     * Is the visualization active or not. Should only be set by the UI.
     */
    var visualizationEnabled = false

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
     * Override this function to actually paint your visualization.
     */
    protected override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
    }

}