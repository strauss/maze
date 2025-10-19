package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.event.*
import javax.swing.JPanel
import kotlin.math.min

class MazePanel(private val controller: UiController) : JPanel() {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MazePanel::class.java)
        private const val INITIAL_ZOOM = 17
        private const val MIN_ZOOM = 5
        private const val MAX_ZOOM = 31
    }

    var zoom: Int = INITIAL_ZOOM
        private set(value) {
            if (value in MIN_ZOOM..MAX_ZOOM) {
                field = value
                // TODO: Glasspane zoom
                if (controller.mazeModel.mazeReceived) {
                    // TODO: Try without because we can center
                    offset.x = min(offset.x, getWidth() - 10 * value).coerceAtLeast(-(controller.mazeModel.width - 10) * value)
                    offset.y = min(offset.y, getHeight() - 10 * value).coerceAtLeast(-(controller.mazeModel.height - 10) * value)
                }
            }
        }

    private lateinit var image: Image

    var imageZoom = 0
        private set

    private var offset: Point = Point(0, 0)

    private var pressPoint: Point? = null

    init {
        minimumSize = Dimension(12 * zoom, 9 * zoom)
        preferredSize = Dimension(45 * zoom, 35 * zoom)

        val mouseListener: MouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (controller.mazeModel.mazeReceived && e?.button == MouseEvent.BUTTON1 && e.clickCount == 2) {
                    centerMaze()
                }
            }

            /**
             * For dragging the maze, we store the source point, where we started.
             */
            override fun mousePressed(e: MouseEvent?) {
                pressPoint = e?.point
            }

            /**
             * When releasing the mouse, we delete the point.
             */
            override fun mouseReleased(e: MouseEvent?) {
                pressPoint = null
            }
        }
        val mouseMotionListener: MouseMotionListener = object : MouseMotionAdapter() {
            /**
             * The real drag action begins here :-D
             */
            override fun mouseDragged(e: MouseEvent?) {
                if (controller.mazeModel.mazeReceived && pressPoint != null && e != null) {
                    offset.x += e.getX() - pressPoint!!.x
                    offset.y += e.getY() - pressPoint!!.y
                    pressPoint = e.getPoint()

                    offset.x = min(offset.x, getWidth() - 10 * zoom)
                        .coerceAtLeast(-(controller.mazeModel.width - 10) * zoom)
                    offset.y = min(offset.y, getHeight() - 10 * zoom)
                        .coerceAtLeast(-(controller.mazeModel.height - 10) * zoom)
                    repaint()
                }
            }
        }
        addMouseListener(mouseListener)
        addMouseMotionListener(mouseMotionListener)


        // Change zoom with mouse wheel ... we don't need no dialog shenanigans
        addMouseWheelListener { e ->
            if (controller.mazeModel.mazeReceived) {
                val rotation = e?.wheelRotation ?: 0
                if (rotation != 0) {
                    zoom -= rotation * 2 // stick to odd numbers and zoom faster than before
                    centerMaze()
                }
            }
        }
    }

    override fun paint(g: Graphics) {
        if (controller.mazeModel.mazeReceived) {
            super.paint(g)
            checkImage()
            g.drawImage(image, offset.x, offset.y, this)
        }
    }

    internal fun checkImage() {
        if (controller.mazeModel.mazeReceived && !this::image.isInitialized || imageZoom != zoom) {
            imageZoom = zoom
            image = createImage(controller.mazeModel.width * zoom, controller.mazeModel.height * zoom)
            paintCompleteMaze(image.graphics)
        }
    }

    private fun paintCompleteMaze(g: Graphics) {
        val maze: MazeModel = controller.mazeModel
        for (y in 0..<maze.height) {
            for (x in 0..<maze.width) {
                val mazeField: MazeModel.Companion.MazeField = maze[x, y]
                internalUpdatePosition(g, x, y, mazeField)
            }
        }
    }

    fun updatePosition(x: Int, y: Int) {
        checkImage()
        val g: Graphics = image.graphics
        val mazeField: MazeModel.Companion.MazeField = controller.mazeModel[x, y]
        internalUpdatePosition(g, x, y, mazeField)
    }

    private fun internalUpdatePosition(g: Graphics, x: Int, y: Int, mazeField: MazeModel.Companion.MazeField) {
        when (mazeField) {
            is MazeModel.Companion.PathMazeField -> drawPath(g, x, y, mazeField)
            is MazeModel.Companion.WallMazeField -> DrawableWall.drawAt(g, x, y, zoom)
            is MazeModel.Companion.OutsideMazeField -> DrawableOutside.drawAt(g, x, y, zoom)
            is MazeModel.Companion.UnknownMazeField -> DrawableUnknown.drawAt(g, x, y, zoom)
        }
    }

    private fun drawPath(g: Graphics, x: Int, y: Int, mazeField: MazeModel.Companion.PathMazeField) {
        // always draw the path, no matter what is on it
        DrawablePath.drawAt(g, x, y, zoom)
        when (mazeField.occupationStatus) {
            MazeModel.Companion.PathOccupationStatus.EMPTY -> {
                // we already drew the path, so we skip here
            }

            MazeModel.Companion.PathOccupationStatus.BAIT -> drawBait(mazeField, g, x, y)
            MazeModel.Companion.PathOccupationStatus.PLAYER -> drawPlayer(mazeField, g, x, y)
            MazeModel.Companion.PathOccupationStatus.CROWDED -> {
                LOGGER.warn("Crowded field detected at ($x : $y).")
                drawBait(mazeField, g, x, y)
                drawPlayer(mazeField, g, x, y)
            }
        }
    }

    private fun drawPlayer(mazeField: MazeModel.Companion.PathMazeField, g: Graphics, x: Int, y: Int) {
        val player: PlayerSnapshot? = mazeField.player
        player?.let { p ->
            val playerColor: Color? = controller.uiPlayerCollection.getById(p.id)?.color
            playerColor?.let { color ->
                DrawablePlayer(color, p.viewDirection).drawAt(g, x, y, zoom)
            }
        }
    }

    private fun drawBait(mazeField: MazeModel.Companion.PathMazeField, g: Graphics, x: Int, y: Int) {
        mazeField.bait?.drawable()?.drawAt(g, x, y, zoom)
    }

    private fun centerMaze() {
        offset.x = ((getWidth() - controller.mazeModel.width * zoom) / 2)
            .coerceAtMost(getWidth() - 10 * zoom)
            .coerceAtLeast(-(controller.mazeModel.width - 10) * zoom)
        offset.y = ((getHeight() - controller.mazeModel.height * zoom) / 2)
            .coerceAtMost(getHeight() - 10 * zoom)
            .coerceAtLeast(-(controller.mazeModel.height - 10) * zoom)
        repaint()
    }

}