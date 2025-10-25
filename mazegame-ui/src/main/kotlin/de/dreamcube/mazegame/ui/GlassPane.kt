package de.dreamcube.mazegame.ui

import java.awt.*
import javax.swing.JComponent
import javax.swing.SwingUtilities

class GlassPane(val mazePanel: MazePanel) : JComponent() {
    private val zoom
        get() = mazePanel.zoom
    private val offset
        get() = mazePanel.offset
    private val qualityHints: RenderingHints = createRenderingHints(
        RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON,
        RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY
    )
    internal var playerToMark: UiPlayerInformation? = null

    protected override fun paintComponent(g: Graphics?) {
        val player = playerToMark ?: return
        val origin = SwingUtilities.convertPoint(mazePanel, Point(0, 0), this)
        val playerPos = Point(offset.x + player.snapshot.view.x * zoom, offset.y + player.snapshot.view.y * zoom)
        val markerPos = SwingUtilities.convertPoint(mazePanel, playerPos, this)
        val g2 = g as Graphics2D
        g2.run {
            setClip(origin.x, origin.y, mazePanel.width, mazePanel.height)
            setRenderingHints(qualityHints)
            color = player.markerColor
            stroke = BasicStroke(4.0f)
            drawOval(markerPos.x - zoom, markerPos.y - zoom, 3 * zoom, 3 * zoom)
        }
    }

    internal fun markPlayer(player: UiPlayerInformation) {
        playerToMark = player
        mazePanel.controller.updatePositionStatus(player.snapshot.x, player.snapshot.y)
        repaint()
    }

    internal fun clearMark() {
        playerToMark = null
        mazePanel.controller.updatePositionStatus(-1, -1)
        repaint()
    }

    internal fun reset() {
        clearMark()
    }

}

internal fun createRenderingHints(vararg configuration: Pair<RenderingHints.Key, Any>): RenderingHints {
    val result = RenderingHints(emptyMap<RenderingHints.Key, Any>())
    for ((key, value) in configuration) {
        result.put(key, value)
    }
    return result
}
