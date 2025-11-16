package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.common.util.VisualizationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.*
import javax.swing.JComponent
import javax.swing.SwingUtilities

private val HOVER_COLOR = Color(0, 0, 0, 42)

class MarkerPane() : JComponent(), PlayerSelectionListener, MazeCellListener {
    private val mazePanel: MazePanel
        get() = UiController.mazePanel

    private val zoom
        get() = mazePanel.zoom

    private val offset
        get() = mazePanel.offset

    var hoverHandle: Deferred<Unit>? = null

    private var hoverX = -1

    private var hoverY = -1

    private val qualityHints: RenderingHints = VisualizationHelper.createDefaultRenderingHints()

    internal var playerToMark: UiPlayerInformation? = null

    protected override fun paintComponent(g: Graphics?) {
        val g2 = g as Graphics2D
        drawPlayerMark(g2)
        drawHoverMark(g2)
    }

    private fun drawPlayerMark(g2: Graphics2D) {
        val player = playerToMark ?: return
        val origin = SwingUtilities.convertPoint(mazePanel, Point(0, 0), this)
        val playerPos = Point(offset.x + player.snapshot.view.x * zoom, offset.y + player.snapshot.view.y * zoom)
        val markerPos = SwingUtilities.convertPoint(mazePanel, playerPos, this)
        g2.run {
            setClip(origin.x, origin.y, mazePanel.width, mazePanel.height)
            setRenderingHints(qualityHints)
            color = player.markerColor
            stroke = BasicStroke(4.0f)
            drawOval(markerPos.x - zoom, markerPos.y - zoom, 3 * zoom, 3 * zoom)
        }
    }

    private fun drawHoverMark(g2: Graphics2D) {
        g2.run {
            if (hoverX >= 0 && hoverY >= 0) {
                color = HOVER_COLOR
                val hoverPos = SwingUtilities.convertPoint(
                    mazePanel,
                    Point(offset.x + hoverX * zoom, offset.y + hoverY * zoom),
                    this@MarkerPane
                )
                fillRect(hoverPos.x + 1, hoverPos.y + 1, zoom - 1, zoom - 1)
            }
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

    override fun onMazeCellSelected(
        x: Int,
        y: Int,
        mazeField: MazeModel.MazeField
    ) {
        // Do nothing
    }

    override fun onMazeCellHovered(x: Int, y: Int, mazeField: MazeModel.MazeField) {
        if (mazeField !is MazeModel.PathMazeField) {
            return
        }
        hoverHandle?.cancel()
        hoverX = x
        hoverY = y
        hoverHandle = UiController.bgScope.async {
            withContext(Dispatchers.Swing) {
                repaint()
            }
            delay(1000L)
            hoverX = -1
            hoverY = -1
            hoverHandle = null
            withContext(Dispatchers.Swing) {
                repaint()
            }
        }
    }

}
