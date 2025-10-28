package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.JPanel
import kotlin.math.min

class MazePanel() : JPanel() {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MazePanel::class.java)
        internal const val INITIAL_ZOOM = 17
        private const val MIN_ZOOM = 5
        private const val MAX_ZOOM = 31
    }

    var zoom: Int = INITIAL_ZOOM
        private set(value) {
            if (value in MIN_ZOOM..MAX_ZOOM) {
                field = value
                UiController.glassPane.repaint()
                UiController.updateZoom(value)
            }
        }

    private lateinit var image: Image

    var imageZoom = 0
        private set

    internal var offset: Point = Point(0, 0)
        private set

    private var pressPoint: Point? = null

    private val mazeCellSelectionListeners: MutableList<MazeCellSelectionListener> = LinkedList()

    internal fun reset() {
        // Erase the image with a transparent rectangle
        image.graphics.color = Color(0xff, 0xff, 0xff, 0x00)
        image.graphics.drawRect(0, 0, width * zoom, height * zoom)
        repaint()

        // change all values
        zoom = INITIAL_ZOOM
        imageZoom = 0
        offset = Point(0, 0)
        pressPoint = null
        UiController.glassPane.reset()
    }

    init {
        UiController.mazePanel = this
        minimumSize = Dimension(12 * zoom, 9 * zoom)
        preferredSize = Dimension(45 * zoom, 35 * zoom)

        val mouseListener: MouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (UiController.mazeModel.mazeReceived && e?.button == MouseEvent.BUTTON1 && e.clickCount == 2) {
                    centerMaze()
                    return
                }
                if (UiController.mazeModel.mazeReceived && e?.button == MouseEvent.BUTTON1 && e.clickCount == 1) {
                    val x = (e.x - offset.x) / zoom
                    val y = (e.y - offset.y) / zoom
                    if (x >= 0 && y >= 0 && x < UiController.mazeModel.width && y < UiController.mazeModel.height) {
                        fireMazeCellSelection(x, y)
                    }
                }
            }

            /**
             * For dragging the maze, we store the source point, where we started.
             */
            override fun mousePressed(e: MouseEvent?) {
                if (e != null && e.button != MouseEvent.BUTTON1) {
                    pressPoint = e.point
                }
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
                if (e != null && UiController.mazeModel.mazeReceived && pressPoint != null) {
                    offset.x += e.getX() - pressPoint!!.x
                    offset.y += e.getY() - pressPoint!!.y
                    pressPoint = e.getPoint()

                    offset.x = min(offset.x, getWidth() - 10 * zoom)
                        .coerceAtLeast(-(UiController.mazeModel.width - 10) * zoom)
                    offset.y = min(offset.y, getHeight() - 10 * zoom)
                        .coerceAtLeast(-(UiController.mazeModel.height - 10) * zoom)
                    repaint()
                }
            }

            override fun mouseMoved(e: MouseEvent?) {
                if (UiController.glassPane.playerToMark == null) {
                    val x = (e?.x ?: 0) - offset.x
                    val y = (e?.y ?: 0) - offset.y
                    UiController.updatePositionStatus(x / zoom, y / zoom)
                }
            }
        }
        addMouseListener(mouseListener)
        addMouseMotionListener(mouseMotionListener)


        // Change zoom with mouse wheel ... we don't need no dialog shenanigans
        addMouseWheelListener { e ->
            if (UiController.mazeModel.mazeReceived) {
                val rotation = e?.wheelRotation ?: 0
                if (rotation != 0) {
                    zoom -= rotation * 2 // stick to odd numbers and zoom faster than before
                }
            }
        }
    }

    override fun paint(g: Graphics) {
        if (UiController.mazeModel.mazeReceived) {
            super.paint(g)
            checkImage()
            g.drawImage(image, offset.x, offset.y, this)
        }
    }

    internal fun checkImage() {
        if (UiController.mazeModel.mazeReceived && !this::image.isInitialized || imageZoom != zoom) {
            imageZoom = zoom
            image = createImage(UiController.mazeModel.width * zoom, UiController.mazeModel.height * zoom)
            centerMaze()
            paintCompleteMaze(image.graphics)
        }
    }

    private fun paintCompleteMaze(g: Graphics) {
        val maze: MazeModel = UiController.mazeModel
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
        val mazeField: MazeModel.Companion.MazeField = UiController.mazeModel[x, y]
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
            val playerColor: Color? = UiController.uiPlayerCollection.getById(p.id)?.color
            playerColor?.let { color ->
                DrawablePlayer(color, p.viewDirection).drawAt(g, x, y, zoom)
            }
        }
    }

    private fun drawBait(mazeField: MazeModel.Companion.PathMazeField, g: Graphics, x: Int, y: Int) {
        mazeField.bait?.drawable()?.drawAt(g, x, y, zoom)
    }

    private fun centerMaze() {
        offset.x = ((getWidth() - UiController.mazeModel.width * zoom) / 2)
            .coerceAtMost(getWidth() - 10 * zoom)
            .coerceAtLeast(-(UiController.mazeModel.width - 10) * zoom)
        offset.y = ((getHeight() - UiController.mazeModel.height * zoom) / 2)
            .coerceAtMost(getHeight() - 10 * zoom)
            .coerceAtLeast(-(UiController.mazeModel.height - 10) * zoom)
        repaint()
    }

    internal fun addMazeCellSelectionListener(mazeCellSelectionListener: MazeCellSelectionListener) {
        mazeCellSelectionListeners.add(mazeCellSelectionListener)
    }

    internal fun removeMazeCellSelectionListener(mazeCellSelectionListener: MazeCellSelectionListener) {
        mazeCellSelectionListeners.remove(mazeCellSelectionListener)
    }

    internal fun fireMazeCellSelection(x: Int, y: Int) {
        for (mazeCellSelectionListener in mazeCellSelectionListeners) {
            mazeCellSelectionListener.onMazeCellSelected(x, y)
        }
    }

}