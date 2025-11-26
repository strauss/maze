package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.ViewDirection
import de.dreamcube.mazegame.common.util.VisualizationHelper
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import kotlin.math.floor

/**
 * Unites all drawable maze objects in one interface. The coordinates are variable. The implementations are "static" in
 * most cases ("object").
 */
sealed interface DrawableMazeObject {
    /**
     * Draws this maze object into the given [Graphics] [g]. [x] and [y] refer to maze coordinates. [zoom] is the zoom factor.
     */
    fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int)
}

/**
 * A path ... usually just a plain white box.
 */
object DrawablePath : DrawableMazeObject {
    internal val PATH_COLOR = Color.white

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        g.color = PATH_COLOR
        g.fillRect(x * zoom, y * zoom, zoom, zoom)
    }
}

/**
 * The visual representation of a wall.
 */
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

/**
 * The outside world ... which is sometimes inside.
 */
object DrawableOutside : DrawableMazeObject {
    private val OUTSIDE_COLOR = Color.darkGray

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        g.color = OUTSIDE_COLOR
        g.fillRect(x * zoom, y * zoom, zoom, zoom)
    }
}

/**
 * Currently mostly unused ... was planned for "fog of war" (not a feature yet). Currently it is a red box, but this
 * might change in the future.
 */
object DrawableUnknown : DrawableMazeObject {
    private val UNKNOWN_COLOR = Color.red

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        g.color = UNKNOWN_COLOR
        g.fillRect(x * zoom, y * zoom, zoom, zoom)
    }
}

/**
 * Abstract representation of a bait. A Bait is represented by a rendered letter. This class actually renders it. The
 * letter is defined by the specializations.
 */
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

/**
 * Gives the drawable representation of a [BaitType].
 */
fun BaitType.drawable(): DrawableBait = when (this) {
    BaitType.FOOD -> DrawableFood
    BaitType.COFFEE -> DrawableCoffee
    BaitType.GEM -> DrawableGem
    BaitType.TRAP -> DrawableTrap
}

/**
 * The only [DrawableMazeObject] that is somewhat variable. Each player has its own instance. This is mainly because of
 * the player's color and viewDirection. It is constantly recreated for all players, because color and view direction
 * regularly change.
 *
 * The player is drawn with an outline, using "areas" as technique.
 */
class DrawablePlayer(private val color: Color, private val viewDirection: ViewDirection) : DrawableMazeObject {
    companion object {
        private val EDGE_COLOR: Color = Color.black
    }

    override fun drawAt(g: Graphics, x: Int, y: Int, zoom: Int) {
        val g2 = g as Graphics2D
        g2.run {
            val zd = zoom.toDouble()
            val xx: Double = x.toDouble() * zd + 1.0
            val yy: Double = y.toDouble() * zd + 1.0
            val wh: Double = zd - 2.0
            val r: Double = floor(wh / 3.0)
            val w: Double = (wh - r) / 2.0

            val rect = Rectangle2D.Double(xx, yy, wh, wh)
            val subRect = when (viewDirection) {
                ViewDirection.NORTH -> Rectangle2D.Double(xx + w, yy, r, r)
                ViewDirection.EAST -> Rectangle2D.Double(xx + zd - 2.0 - r, yy + w, r, r)
                ViewDirection.SOUTH -> Rectangle2D.Double(xx + w, yy + zd - 2.0 - r, r, r)
                ViewDirection.WEST -> Rectangle2D.Double(xx, yy + w, r, r)
            }
            val playerArea = Area(rect).apply { subtract(Area(subRect)) }
            color = this@DrawablePlayer.color
            g2.fill(playerArea)
            color = EDGE_COLOR
            g2.draw(playerArea)
        }
    }

}
