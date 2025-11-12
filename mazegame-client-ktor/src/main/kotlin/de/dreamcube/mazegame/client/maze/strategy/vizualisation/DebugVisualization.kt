package de.dreamcube.mazegame.client.maze.strategy.vizualisation

import de.dreamcube.mazegame.client.maze.strategy.VisualizationComponent
import de.dreamcube.mazegame.common.util.VisualizationHelper.createDefaultRenderingHints
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D

/**
 * Example visualization. It showcases how to get the color of the selected player and how to access some of the other
 * fields from [VisualizationComponent].
 */
class DebugVisualization : VisualizationComponent() {

    private val qualityHints = createDefaultRenderingHints()

    override fun paintComponent(g: Graphics?) {
        val g2 = g as Graphics2D
        g2.run {
            setRenderingHints(qualityHints)
            color = selectedPlayerId?.let { getPlayerColor(it) } ?: Color.magenta
            font = g2.font.deriveFont(50.0f)
            drawString("Debug visualization", 0, 50)
            drawString("Offset: (${offset.x}/${offset.y})", 0, 100)
            drawString("Zoom: $zoom", 0, 150)
            drawRect(0, 0, width - 1, height - 1)
        }
    }
}