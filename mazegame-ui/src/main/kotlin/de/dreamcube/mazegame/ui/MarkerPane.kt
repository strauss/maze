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

import de.dreamcube.mazegame.common.util.VisualizationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.*
import javax.swing.JComponent
import javax.swing.SwingUtilities

private val HOVER_COLOR = Color(0, 0, 0, 42)

/**
 * Part of the glass pane that is always active. It is responsible for highlighting the marked player. It also handles
 * the marking of cells that are hovered, if this is required (e.g., when placing a bait using the server control).
 */
class MarkerPane() : JComponent(), PlayerSelectionListener, MazeCellListener {
    /**
     * Reference to the maze panel.
     */
    private val mazePanel: MazePanel
        get() = UiController.mazePanel

    /**
     * The current zoom.
     */
    private val zoom
        get() = mazePanel.zoom

    /**
     * The current offset (origin of the top-left pixel of the maze).
     */
    private val offset
        get() = mazePanel.offset

    /**
     * Required for the "fancy hover mechanism". Allows for cancelling the erasure of the highlighted cell.
     */
    var hoverHandle: Deferred<Unit>? = null

    /**
     * X-coordinate of the cell the mouse is hovering over.
     */
    private var hoverX = -1

    /**
     * Y-coordinate of the cell the mouse is hovering over.
     */
    private var hoverY = -1

    private val qualityHints: RenderingHints = VisualizationHelper.createDefaultRenderingHints()

    /**
     * [UiPlayerInformation] of the marked player, if there is any.
     */
    internal var playerToMark: UiPlayerInformation? = null

    protected override fun paintComponent(g: Graphics?) {
        val g2 = g as Graphics2D
        drawPlayerMark(g2)
        drawHoverMark(g2)
    }

    /**
     * Draws a circle in "player color" around the marked player if there is one.
     */
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

    /**
     * Draws a semi-transparent black rectangle above the cell that should be highlighted.
     */
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

    /**
     * Sets the marked player and repaints this component.
     */
    private fun markPlayer(player: UiPlayerInformation) {
        playerToMark = player
        UiController.updatePositionStatus(player.snapshot.x, player.snapshot.y)
        repaint()
    }

    /**
     * Clears the mark and repaints this component.
     */
    private fun clearMark() {
        playerToMark = null
        UiController.updatePositionStatus(-1, -1)
        repaint()
    }

    /**
     * On UI reset, the mark is also cleared.
     */
    internal fun reset() {
        clearMark()
    }

    /**
     * When a player is selected, it is marked.
     */
    override fun onPlayerSelected(player: UiPlayerInformation) {
        markPlayer(player)
    }

    /**
     * When the selected player is deselected, the mark is cleared.
     */
    override fun onPlayerSelectionCleared() {
        clearMark()
    }

    /**
     * We do nothing if a cell is selected.
     */
    override fun onMazeCellSelected(
        x: Int,
        y: Int,
        mazeField: MazeModel.MazeField
    ) {
        // Do nothing
    }

    /**
     * If the mouse hovers above a maze cell (if it is moving above it), we briefly highlight the cell and use an async
     * call to clear the mark after a short amount of time. If a [hoverHandle] is still active, it is canceled. This is
     * required, because otherwise a highlight might be cleared too early.
     */
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
            // negative values result in "no highlight"
            hoverX = -1
            hoverY = -1
            hoverHandle = null
            withContext(Dispatchers.Swing) {
                repaint()
            }
        }
    }

}
