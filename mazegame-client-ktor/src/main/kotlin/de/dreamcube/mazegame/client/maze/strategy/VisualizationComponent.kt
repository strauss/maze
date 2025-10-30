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

    /**
     * The offset of the origin point (top left corner). Should only be set by the UI.
     */
    var offset: Point = Point(0, 0)

    /**
     * Override this function to actually paint your visualization.
     */
    protected override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
    }

}