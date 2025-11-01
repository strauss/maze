package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.common.util.VisualizationHelper
import java.awt.*
import javax.swing.JComponent
import javax.swing.SwingUtilities

class MarkerPane() : JComponent(), PlayerSelectionListener {
    private val mazePanel: MazePanel
        get() = UiController.mazePanel

    private val zoom
        get() = mazePanel.zoom

    private val offset
        get() = mazePanel.offset

    private val qualityHints: RenderingHints = VisualizationHelper.createDefaultRenderingHints()

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

    private fun markPlayer(player: UiPlayerInformation) {
        playerToMark = player
        UiController.updatePositionStatus(player.snapshot.x, player.snapshot.y)
        repaint()
    }

    private fun clearMark() {
        playerToMark = null
        UiController.updatePositionStatus(-1, -1)
        repaint()
    }

    internal fun reset() {
        clearMark()
    }

    override fun onPlayerSelected(player: UiPlayerInformation) {
        markPlayer(player)
    }

    override fun onPlayerSelectionCleared() {
        clearMark()
    }

}
