package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.ViewDirection
import de.dreamcube.mazegame.common.util.VisualizationHelper
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D

interface DrawableMazeObject {
    /**
     * Draws this maze object into the given [Graphics] [g]. [x] and [y] refer to maze coordinates. [zoom] is the zoom factor.
     */
    fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int)
}

object DrawablePath : DrawableMazeObject {
    internal val PATH_COLOR = Color.white

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        g.color = PATH_COLOR
        g.fillRect(x * zoom, y * zoom, zoom, zoom)
    }
}

object DrawableWall : DrawableMazeObject {
    private val WALL_COLOR = Color.lightGray

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        g.color = WALL_COLOR
        g.fillRect(x * zoom, y * zoom, zoom - 1, zoom - 1)
        g.color = WALL_COLOR.brighter()
        g.drawLine(x * zoom, y * zoom, x * zoom + zoom - 1, y * zoom)
        g.drawLine(x * zoom, y * zoom, x * zoom, y * zoom + zoom - 1)
        g.color = WALL_COLOR.darker()
        g.drawLine(
            x * zoom + zoom - 1, y * zoom + 1, x * zoom + zoom - 1,
            y * zoom + zoom
        )
        g.drawLine(
            x * zoom + 1, y * zoom + zoom - 1, x * zoom + zoom - 1,
            y * zoom + zoom - 1
        )
    }
}

object DrawableOutside : DrawableMazeObject {
    private val OUTSIDE_COLOR = Color.darkGray

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        g.color = OUTSIDE_COLOR
        g.fillRect(x * zoom, y * zoom, zoom, zoom)
    }
}

object DrawableUnknown : DrawableMazeObject {
    private val UNKNOWN_COLOR = Color.red

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        g.color = UNKNOWN_COLOR
        g.fillRect(x * zoom, y * zoom, zoom, zoom)
    }
}

abstract class DrawableBait(private val type: BaitType) : DrawableMazeObject {
    companion object {
        protected val BAIT_BASE_FONT = Font("SansSerif", Font.BOLD, 0)
    }

    protected abstract val baitColor: Color

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        val baitFont = Font(BAIT_BASE_FONT.name, BAIT_BASE_FONT.style, zoom - 1)
        g.font = baitFont
        g.color = baitColor
        VisualizationHelper.drawTextCentric(g as Graphics2D, "${type.asChar}", x * zoom, y * zoom, zoom, zoom)
    }
}

object DrawableCoffee : DrawableBait(BaitType.COFFEE) {
    override val baitColor: Color = Color.red.darker()
}

object DrawableFood : DrawableBait(BaitType.FOOD) {
    override val baitColor: Color = Color.green.darker()
}

object DrawableGem : DrawableBait(BaitType.GEM) {
    override val baitColor: Color = Color.blue
}

object DrawableTrap : DrawableBait(BaitType.TRAP) {
    override val baitColor: Color = Color.darkGray
}

fun BaitType.drawable(): DrawableBait = when (this) {
    BaitType.FOOD -> DrawableFood
    BaitType.COFFEE -> DrawableCoffee
    BaitType.GEM -> DrawableGem
    BaitType.TRAP -> DrawableTrap
}

class DrawablePlayer(private val color: Color, private val viewDirection: ViewDirection) : DrawableMazeObject {
    companion object {
        private val EDGE_COLOR: Color = Color.black
    }

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        val w: Int
        g.color = color

        val xx: Int = x * zoom + 1
        val yy: Int = y * zoom + 1
        g.fillRect(xx, yy, zoom - 2, zoom - 2)
        val r = zoom / 3
        w = (zoom - r - 2) / 2
        g.color = DrawablePath.PATH_COLOR

        when (viewDirection) {
            ViewDirection.NORTH -> cutNorth(g, xx, yy, w, r, zoom)
            ViewDirection.EAST -> cutEast(g, xx, yy, w, r, zoom)
            ViewDirection.SOUTH -> cutSouth(g, xx, yy, w, r, zoom)
            ViewDirection.WEST -> cutWest(g, xx, yy, w, r, zoom)

        }
    }

    private fun cutNorth(g: Graphics, x: Int, y: Int, w: Int, r: Int, zoom: Int) {
        val dx = w
        val dy = 0
        g.fillRect(x + dx, y + dy, r, r)

        g.color = EDGE_COLOR

        // north
        g.drawLine(x, y, x + w, y) // right
        g.drawLine(x + w, y, x + w, y + w) // down
        g.drawLine(x + w, y + w, x + w + r, y + w) // further right
        g.drawLine(x + w + r, y + w, x + w + r, y) // up again
        g.drawLine(x + w + r, y, x + zoom - 2, y) // finalize to the right

        // east
        g.drawLine(x + zoom - 2, y, x + zoom - 2, y + zoom - 2)

        // south
        g.drawLine(x + zoom - 2, y + zoom - 2, x, y + zoom - 2)

        // west
        g.drawLine(x, y + zoom - 2, x, y)
    }

    private fun cutEast(g: Graphics, x: Int, y: Int, w: Int, r: Int, zoom: Int) {
        val dx = zoom - 1 - r
        val dy = w
        g.fillRect(x + dx, y + dy, r, r)

        g.color = EDGE_COLOR

        // north
        g.drawLine(x, y, x + zoom - 2, y)

        // east
        g.drawLine(x + zoom - 2, y, x + zoom - 2, y + w) // down
        g.drawLine(x + zoom - 2, y + w, x + zoom - 2 - w, y + w) // left
        g.drawLine(x + zoom - 2 - w, y + w, x + zoom - 2 - w, y + w + r) // further down
        g.drawLine(x + zoom - 2 - w, y + w + r, x + zoom - 2, y + w + r) // right again
        g.drawLine(x + zoom - 2, y + w + r, x + zoom - 2, y + zoom - 2) // finalize to the bottom

        // south
        g.drawLine(x + zoom - 2, y + zoom - 2, x, y + zoom - 2)

        // west
        g.drawLine(x, y + zoom - 2, x, y)
    }

    private fun cutSouth(g: Graphics, x: Int, y: Int, w: Int, r: Int, zoom: Int) {
        val dx = w
        val dy = zoom - 1 - r
        g.fillRect(x + dx, y + dy, r, r)

        g.color = EDGE_COLOR

        // north
        g.drawLine(x, y, x + zoom - 2, y)

        // east
        g.drawLine(x + zoom - 2, y, x + zoom - 2, y + zoom - 2)

        // south
        g.drawLine(x, y + zoom - 2, x + w, y + zoom - 2) // right
        g.drawLine(x + w, y + zoom - 2, x + w, y + zoom - 2 - w) // down
        g.drawLine(x + w, y + zoom - 2 - w, x + w + r, y + zoom - 2 - w) // further right
        g.drawLine(x + w + r, y + zoom - 2 - w, x + w + r, y + zoom - 2) // up again
        g.drawLine(x + w + r, y + zoom - 2, x + zoom - 2, y + zoom - 2) // finalize to the right

        // west
        g.drawLine(x, y + zoom - 2, x, y)
    }

    private fun cutWest(g: Graphics, x: Int, y: Int, w: Int, r: Int, zoom: Int) {
        val dx = 0
        val dy = w
        g.fillRect(x + dx, y + dy, r, r)

        g.color = EDGE_COLOR

        // north
        g.drawLine(x, y, x + zoom - 2, y)

        // east
        g.drawLine(x + zoom - 2, y, x + zoom - 2, y + zoom - 2)

        // south
        g.drawLine(x + zoom - 2, y + zoom - 2, x, y + zoom - 2)

        // west
        g.drawLine(x, y, x, y + w) // down
        g.drawLine(x, y + w, x + w, y + w) // left
        g.drawLine(x + w, y + w, x + w, y + w + r) // further down
        g.drawLine(x + w, y + w + r, x, y + w + r) // right again
        g.drawLine(x, y + w + r, x, y + zoom - 2) // finalize to the bottom
    }

}

fun UiPlayerInformation.drawable() = DrawablePlayer(this.color, this.snapshot.viewDirection)
